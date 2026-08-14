package com.thx.module.file.service.impl;

import cn.hutool.core.io.file.FileNameUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.thx.common.util.UUIDUtil;
import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.enums.FileAccessLevel;
import com.thx.module.file.enums.FileOperation;
import com.thx.module.file.enums.FileStatus;
import com.thx.module.file.exception.FileSystemException;
import com.thx.module.file.mapper.FileAssetMapper;
import com.thx.module.file.model.FileAsset;
import com.thx.module.file.model.FilePolicy;
import com.thx.module.file.service.FileAuditService;
import com.thx.module.file.service.FileAuthService;
import com.thx.module.file.service.FileCleanupService;
import com.thx.module.file.service.FilePolicyService;
import com.thx.module.file.service.FileQuotaService;
import com.thx.module.file.service.FileSystemService;
import com.thx.module.file.service.FileUrlService;
import com.thx.module.file.storage.ObjectStorageClient;
import com.thx.module.file.storage.StoragePutResult;
import com.thx.module.file.util.FileHashUtil;
import com.thx.module.file.util.MimeDetector;
import com.thx.module.file.util.ObjectKeyGenerator;
import com.thx.module.file.vo.FileInfoResult;
import com.thx.module.file.vo.FileUploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 * 文件系统核心门面实现
 * 上传流程：校验 Policy -> 校验 Owner -> 校验配额 -> 生成 fileId/objectKey ->
 * 单次读取同时计算 SHA256 并上传 -> 写 file_asset -> 记录审计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileSystemServiceImpl implements FileSystemService {

    /** file_asset.storage_provider 固定取值，当前只支持 MinIO */
    private static final String STORAGE_PROVIDER_MINIO = "MINIO";
    /** 审计日志的成功结果标记 */
    private static final String RESULT_SUCCESS = "SUCCESS";
    /** 审计日志的失败结果标记 */
    private static final String RESULT_FAIL = "FAIL";

    /** 按 appId+namespace 查找并校验文件策略 */
    private final FilePolicyService filePolicyService;
    /** 校验 App 存储配额是否够用 */
    private final FileQuotaService fileQuotaService;
    /** 校验读取/删除权限（App 隔离 + 访问级别） */
    private final FileAuthService fileAuthService;
    /** 动态生成文件访问 URL */
    private final FileUrlService fileUrlService;
    /** 逻辑删除、补偿删除失败时登记清理任务 */
    private final FileCleanupService fileCleanupService;
    /** 记录操作审计日志 */
    private final FileAuditService fileAuditService;
    /** 实际执行对象存储的上传/删除/预签名 */
    private final ObjectStorageClient objectStorageClient;
    /** 读写 file_asset 表 */
    private final FileAssetMapper fileAssetMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResult upload(MultipartFile file, String namespace, String ownerId, FileCallerContext caller) {
        if (file == null || file.isEmpty()) {
            throw FileSystemException.badRequest("EMPTY_FILE", "文件不能为空");
        }

        FilePolicy policy = filePolicyService.getPolicy(caller.getAppId(), namespace);
        filePolicyService.validate(file, policy);

        FileAccessLevel accessLevel = FileAccessLevel.valueOf(policy.getAccessLevel());
        if (accessLevel == FileAccessLevel.OWNER_ONLY && (ownerId == null || ownerId.trim().isEmpty())) {
            throw FileSystemException.badRequest("OWNER_ID_REQUIRED", "该文件类型必须指定 ownerId");
        }

        fileQuotaService.checkUploadAllowed(caller.getAppId(), file.getSize());

        String fileId = UUIDUtil.uuid();
        String originalName = file.getOriginalFilename();
        String extension = originalName == null ? "" : FileNameUtil.getSuffix(originalName);
        String objectKey = ObjectKeyGenerator.generate(caller.getAppId(), namespace, fileId, extension);
        String detectedMimeType = detectMimeType(file);

        StoragePutResult putResult;
        String sha256;
        MessageDigest digest = FileHashUtil.sha256Digest();
        try (InputStream in = file.getInputStream();
             DigestInputStream digestInputStream = new DigestInputStream(in, digest)) {
            putResult = objectStorageClient.put(policy.getBucket(), objectKey, digestInputStream, file.getSize(), file.getContentType());
            sha256 = FileHashUtil.toHex(digest.digest());
        } catch (IOException e) {
            fileAuditService.log(caller, null, FileOperation.UPLOAD, RESULT_FAIL, "UPLOAD_IO_ERROR", caller.getIp());
            throw new FileSystemException(500, "UPLOAD_FAILED", "文件上传失败");
        }

        FileAsset asset = new FileAsset()
                .setFileId(fileId)
                .setAppId(caller.getAppId())
                .setNamespace(namespace)
                .setPolicyCode(policy.getPolicyCode())
                .setOriginalName(originalName)
                .setExtension(extension)
                .setContentType(file.getContentType())
                .setDetectedMimeType(detectedMimeType)
                .setSize(file.getSize())
                .setSha256(sha256)
                .setStorageProvider(STORAGE_PROVIDER_MINIO)
                .setBucket(putResult.getBucket())
                .setObjectKey(putResult.getObjectKey())
                .setEtag(putResult.getEtag())
                .setAccessLevel(accessLevel.name())
                .setOwnerType(ownerId != null ? "USER" : null)
                .setOwnerId(ownerId)
                .setStatus(FileStatus.ACTIVE.name());

        try {
            fileAssetMapper.insert(asset);
        } catch (Exception e) {
            log.error("写入文件元数据失败，尝试补偿删除已上传的对象: fileId={}", fileId, e);
            compensateDelete(fileId, putResult);
            fileAuditService.log(caller, fileId, FileOperation.UPLOAD, RESULT_FAIL, "METADATA_WRITE_FAILED", caller.getIp());
            throw new FileSystemException(500, "UPLOAD_FAILED", "文件上传失败");
        }

        String url = fileUrlService.resolveUrl(asset);
        fileAuditService.log(caller, fileId, FileOperation.UPLOAD, RESULT_SUCCESS, null, caller.getIp());
        return new FileUploadResult(fileId, originalName, url);
    }

    /** 数据库写入失败后的补偿：删除刚上传的对象；补偿删除也失败则登记 CLEAN_ORPHAN 清理任务 */
    private void compensateDelete(String fileId, StoragePutResult putResult) {
        try {
            objectStorageClient.delete(putResult.getBucket(), putResult.getObjectKey());
        } catch (Exception deleteEx) {
            log.error("补偿删除失败，登记清理任务: fileId={}", fileId, deleteEx);
            fileCleanupService.enqueueCleanupTask(
                    fileId, putResult.getBucket(), putResult.getObjectKey(), "CLEAN_ORPHAN", deleteEx.getMessage());
        }
    }

    /** 用 Tika 检测文件真实内容类型，仅用于落库记录，检测失败不阻断上传 */
    private String detectMimeType(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return MimeDetector.detect(in);
        } catch (IOException e) {
            log.warn("检测文件真实 MIME 类型失败", e);
            return null;
        }
    }

    @Override
    public FileInfoResult get(String fileId, FileCallerContext caller) {
        FileAsset asset = findActiveOrThrow(fileId);
        fileAuthService.checkRead(caller, asset);
        fileAuditService.log(caller, fileId, FileOperation.READ, RESULT_SUCCESS, null, caller.getIp());
        return toInfoResult(asset);
    }

    @Override
    public String getDownloadUrl(String fileId, FileCallerContext caller) {
        FileAsset asset = findActiveOrThrow(fileId);
        fileAuthService.checkRead(caller, asset);
        String url = fileUrlService.resolveUrl(asset);
        fileAuditService.log(caller, fileId, FileOperation.PRESIGN, RESULT_SUCCESS, null, caller.getIp());
        return url;
    }

    @Override
    public void delete(String fileId, FileCallerContext caller) {
        FileAsset asset = fileAssetMapper.selectOne(
                new LambdaQueryWrapper<FileAsset>().eq(FileAsset::getFileId, fileId));
        if (asset == null) {
            throw FileSystemException.notFound("文件不存在");
        }
        fileAuthService.checkDelete(caller, asset);

        if (FileStatus.ACTIVE.name().equals(asset.getStatus())) {
            fileCleanupService.softDelete(asset);
        }
        // 已经处于 DELETED/PURGING/PURGED/PURGE_FAILED 时视为幂等成功，不报错

        fileAuditService.log(caller, fileId, FileOperation.DELETE, RESULT_SUCCESS, null, caller.getIp());
    }

    @Override
    public IPage<FileInfoResult> list(int page, int size, String namespace, FileCallerContext caller) {
        Page<FileAsset> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<FileAsset> wrapper = new LambdaQueryWrapper<FileAsset>()
                .eq(FileAsset::getAppId, caller.getAppId())
                .eq(FileAsset::getStatus, FileStatus.ACTIVE.name())
                .orderByDesc(FileAsset::getCreateTime);
        if (namespace != null && !namespace.trim().isEmpty()) {
            wrapper.eq(FileAsset::getNamespace, namespace.trim());
        }
        IPage<FileAsset> assetPage = fileAssetMapper.selectPage(pageParam, wrapper);
        fileAuditService.log(caller, null, FileOperation.LIST, RESULT_SUCCESS, null, caller.getIp());
        return assetPage.convert(this::toInfoResult);
    }

    /** 按 fileId 查找 ACTIVE 状态的文件，不存在（含已删除）一律按 404 处理 */
    private FileAsset findActiveOrThrow(String fileId) {
        FileAsset asset = fileAssetMapper.selectOne(new LambdaQueryWrapper<FileAsset>()
                .eq(FileAsset::getFileId, fileId)
                .eq(FileAsset::getStatus, FileStatus.ACTIVE.name()));
        if (asset == null) {
            throw FileSystemException.notFound("文件不存在");
        }
        return asset;
    }

    /** 将内部实体转换为对外暴露的元数据 VO，不包含 bucket/objectKey 等存储细节 */
    private FileInfoResult toInfoResult(FileAsset asset) {
        return new FileInfoResult(
                asset.getFileId(),
                asset.getNamespace(),
                asset.getOriginalName(),
                asset.getExtension(),
                asset.getContentType(),
                asset.getSize(),
                asset.getSha256(),
                asset.getAccessLevel(),
                asset.getOwnerId(),
                asset.getStatus(),
                asset.getCreateTime());
    }
}
