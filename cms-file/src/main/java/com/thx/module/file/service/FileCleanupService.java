package com.thx.module.file.service;

import com.thx.module.file.model.FileAsset;

/**
 * 文件删除与物理清理服务
 * 删除接口只做逻辑删除，真正的对象存储删除由定时任务在宽限期后异步执行
 */
public interface FileCleanupService {

    /** 逻辑删除：ACTIVE -> DELETED，记录 deleted_at；已经不是 ACTIVE 时视为幂等成功，不报错 */
    void softDelete(FileAsset asset);

    /** 扫描宽限期已过的 DELETED 文件，CAS 转为 PURGING 后执行物理删除 */
    void purgeDueFiles();

    /** 处理 storage_cleanup_task 中到期需要重试的补偿任务 */
    void retryFailedTasks();

    /** 登记一个清理补偿任务（上传补偿删除失败、或物理清理失败时调用） */
    void enqueueCleanupTask(String fileId, String bucket, String objectKey, String taskType, String lastError);
}
