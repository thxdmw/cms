package com.thx.module.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.thx.module.file.config.FileSystemProperties;
import com.thx.module.file.enums.FileStatus;
import com.thx.module.file.mapper.FileAssetMapper;
import com.thx.module.file.mapper.StorageCleanupTaskMapper;
import com.thx.module.file.model.FileAsset;
import com.thx.module.file.model.StorageCleanupTask;
import com.thx.module.file.service.FileCleanupService;
import com.thx.module.file.storage.ObjectStorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * 文件删除与物理清理实现
 * 状态流转全部使用 CAS（带 WHERE 状态条件的 UPDATE），避免并发重复清理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileCleanupServiceImpl implements FileCleanupService {

    /** 补偿任务重试退避时间表：1分钟、5分钟、30分钟、2小时、6小时、24小时，之后维持 24 小时 */
    private static final long[] BACKOFF_MINUTES = {1, 5, 30, 120, 360, 1440};

    /** last_error 字段的最大保存长度，避免异常堆栈信息把列撑爆 */
    private static final int MAX_ERROR_LENGTH = 1000;

    /** 读写 file_asset 的状态流转（ACTIVE/DELETED/PURGING/PURGED/PURGE_FAILED） */
    private final FileAssetMapper fileAssetMapper;
    /** 读写 storage_cleanup_task 补偿任务 */
    private final StorageCleanupTaskMapper storageCleanupTaskMapper;
    /** 执行真正的对象存储物理删除 */
    private final ObjectStorageClient objectStorageClient;
    /** 读取宽限天数、最大重试次数等清理相关配置 */
    private final FileSystemProperties fileSystemProperties;

    @Override
    public void softDelete(FileAsset asset) {
        FileAsset update = new FileAsset();
        update.setStatus(FileStatus.DELETED.name());
        update.setDeletedAt(new Date());
        int rows = fileAssetMapper.update(update, new LambdaUpdateWrapper<FileAsset>()
                .eq(FileAsset::getFileId, asset.getFileId())
                .eq(FileAsset::getStatus, FileStatus.ACTIVE.name()));
        if (rows == 0) {
            log.debug("文件已经不是 ACTIVE 状态，逻辑删除视为幂等成功: fileId={}", asset.getFileId());
        }
    }

    @Override
    public void purgeDueFiles() {
        FileSystemProperties.Cleanup cleanup = fileSystemProperties.getCleanup();
        if (!cleanup.isEnabled()) {
            return;
        }
        Date threshold = Date.from(Instant.now().minus(cleanup.getGraceDays(), ChronoUnit.DAYS));
        List<FileAsset> dueAssets = fileAssetMapper.selectList(new LambdaQueryWrapper<FileAsset>()
                .eq(FileAsset::getStatus, FileStatus.DELETED.name())
                .lt(FileAsset::getDeletedAt, threshold));
        for (FileAsset asset : dueAssets) {
            purgeOne(asset);
        }
    }

    /** 对单个到期文件执行 CAS 占用 + 物理删除，失败则登记重试任务 */
    private void purgeOne(FileAsset asset) {
        int rows = fileAssetMapper.update(statusOnly(FileStatus.PURGING), new LambdaUpdateWrapper<FileAsset>()
                .eq(FileAsset::getFileId, asset.getFileId())
                .eq(FileAsset::getStatus, FileStatus.DELETED.name()));
        if (rows == 0) {
            // 被其他清理任务并发抢占，跳过
            return;
        }
        try {
            objectStorageClient.delete(asset.getBucket(), asset.getObjectKey());
            fileAssetMapper.update(statusOnly(FileStatus.PURGED), new LambdaUpdateWrapper<FileAsset>()
                    .eq(FileAsset::getFileId, asset.getFileId()));
        } catch (Exception e) {
            log.error("物理清理文件失败: fileId={}", asset.getFileId(), e);
            fileAssetMapper.update(statusOnly(FileStatus.PURGE_FAILED), new LambdaUpdateWrapper<FileAsset>()
                    .eq(FileAsset::getFileId, asset.getFileId()));
            enqueueCleanupTask(asset.getFileId(), asset.getBucket(), asset.getObjectKey(), "DELETE_OBJECT", e.getMessage());
        }
    }

    @Override
    public void enqueueCleanupTask(String fileId, String bucket, String objectKey, String taskType, String lastError) {
        StorageCleanupTask task = new StorageCleanupTask()
                .setFileId(fileId)
                .setBucket(bucket)
                .setObjectKey(objectKey)
                .setTaskType(taskType)
                .setStatus("PENDING")
                .setRetryCount(0)
                .setNextRetryTime(new Date())
                .setLastError(truncate(lastError));
        storageCleanupTaskMapper.insert(task);
    }

    @Override
    public void retryFailedTasks() {
        List<StorageCleanupTask> dueTasks = storageCleanupTaskMapper.selectList(new LambdaQueryWrapper<StorageCleanupTask>()
                .eq(StorageCleanupTask::getStatus, "PENDING")
                .le(StorageCleanupTask::getNextRetryTime, new Date()));
        for (StorageCleanupTask task : dueTasks) {
            processTask(task);
        }
    }

    /** 对单个补偿任务执行 CAS 占用 + 物理删除，失败则按退避策略安排下次重试或标记 FAILED */
    private void processTask(StorageCleanupTask task) {
        int rows = storageCleanupTaskMapper.update(
                new StorageCleanupTask().setStatus("PROCESSING"),
                new LambdaUpdateWrapper<StorageCleanupTask>()
                        .eq(StorageCleanupTask::getId, task.getId())
                        .eq(StorageCleanupTask::getStatus, "PENDING"));
        if (rows == 0) {
            return;
        }
        try {
            objectStorageClient.delete(task.getBucket(), task.getObjectKey());
            storageCleanupTaskMapper.update(
                    new StorageCleanupTask().setStatus("SUCCESS"),
                    new LambdaUpdateWrapper<StorageCleanupTask>().eq(StorageCleanupTask::getId, task.getId()));
            if (task.getFileId() != null) {
                fileAssetMapper.update(statusOnly(FileStatus.PURGED), new LambdaUpdateWrapper<FileAsset>()
                        .eq(FileAsset::getFileId, task.getFileId()));
            }
        } catch (Exception e) {
            log.error("清理补偿任务执行失败: taskId={}, bucket={}, objectKey={}", task.getId(), task.getBucket(), task.getObjectKey(), e);
            int retryCount = task.getRetryCount() + 1;
            int maxRetryCount = fileSystemProperties.getCleanup().getMaxRetryCount();
            StorageCleanupTask update = new StorageCleanupTask()
                    .setRetryCount(retryCount)
                    .setLastError(truncate(e.getMessage()));
            if (retryCount >= maxRetryCount) {
                update.setStatus("FAILED");
            } else {
                update.setStatus("PENDING");
                update.setNextRetryTime(nextRetryTime(retryCount));
            }
            storageCleanupTaskMapper.update(update,
                    new LambdaUpdateWrapper<StorageCleanupTask>().eq(StorageCleanupTask::getId, task.getId()));
        }
    }

    /** 构造一个只设置了 status 的 FileAsset，配合 UpdateWrapper 只更新这一列 */
    private FileAsset statusOnly(FileStatus status) {
        FileAsset asset = new FileAsset();
        asset.setStatus(status.name());
        return asset;
    }

    /** 按 BACKOFF_MINUTES 表计算下一次重试时间，超出表长后固定使用最后一档（24 小时） */
    private Date nextRetryTime(int retryCount) {
        int index = Math.min(retryCount - 1, BACKOFF_MINUTES.length - 1);
        return Date.from(Instant.now().plus(BACKOFF_MINUTES[index], ChronoUnit.MINUTES));
    }

    /** 截断错误信息到 MAX_ERROR_LENGTH，避免写入超长文本 */
    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }
}
