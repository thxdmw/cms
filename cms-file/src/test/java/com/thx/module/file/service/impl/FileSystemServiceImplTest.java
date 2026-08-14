package com.thx.module.file.service.impl;

import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.enums.CallerType;
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
import com.thx.module.file.service.FileUrlService;
import com.thx.module.file.storage.ObjectStorageClient;
import com.thx.module.file.storage.StoragePutResult;
import com.thx.module.file.vo.FileUploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FileSystemServiceImpl 核心门面测试
 * 覆盖上传成功、OWNER_ONLY 缺少 ownerId 拒绝、对象存储写入失败的传播、
 * 元数据写入失败后的补偿删除（含补偿删除本身也失败时登记清理任务）、
 * 以及删除接口的幂等性（ACTIVE 才真正触发软删除，其余状态视为幂等成功）
 */
@ExtendWith(MockitoExtension.class)
class FileSystemServiceImplTest {

    @Mock
    private FilePolicyService filePolicyService;
    @Mock
    private FileQuotaService fileQuotaService;
    @Mock
    private FileAuthService fileAuthService;
    @Mock
    private FileUrlService fileUrlService;
    @Mock
    private FileCleanupService fileCleanupService;
    @Mock
    private FileAuditService fileAuditService;
    @Mock
    private ObjectStorageClient objectStorageClient;
    @Mock
    private FileAssetMapper fileAssetMapper;

    private FileSystemServiceImpl fileSystemService;

    @BeforeEach
    void setUp() {
        fileSystemService = new FileSystemServiceImpl(filePolicyService, fileQuotaService, fileAuthService,
                fileUrlService, fileCleanupService, fileAuditService, objectStorageClient, fileAssetMapper);
    }

    /** 构造测试用的调用方上下文，固定为 cms/user-1，仅拥有 UPLOAD Scope */
    private FileCallerContext caller() {
        FileCallerContext ctx = new FileCallerContext();
        ctx.setAppId("cms");
        ctx.setUserId("user-1");
        ctx.setCallerType(CallerType.SYSTEM);
        ctx.setScopes(Collections.singleton("UPLOAD"));
        return ctx;
    }

    /** 构造一个 PUBLIC 访问级别的文件策略 */
    private FilePolicy publicPolicy() {
        return new FilePolicy().setPolicyCode("PUBLIC_IMAGE").setMaxFileSize(1000L)
                .setAccessLevel("PUBLIC").setBucket("public-assets").setStatus(1);
    }

    /** 构造一个 OWNER_ONLY 访问级别的文件策略 */
    private FilePolicy ownerOnlyPolicy() {
        return new FilePolicy().setPolicyCode("PRIVATE_FILE").setMaxFileSize(1000L)
                .setAccessLevel("OWNER_ONLY").setBucket("private-files").setStatus(1);
    }

    @Test
    void uploadSuccessReturnsUrlFromFileUrlService() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
        when(filePolicyService.getPolicy("cms", "article-image")).thenReturn(publicPolicy());
        when(objectStorageClient.put(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn(new StoragePutResult("public-assets", "apps/cms/article-image/x.png", "etag-1"));
        when(fileUrlService.resolveUrl(any(FileAsset.class))).thenReturn("http://cdn.example.com/x.png");

        FileUploadResult result = fileSystemService.upload(file, "article-image", null, caller());

        assertEquals("http://cdn.example.com/x.png", result.getUrl());
        assertEquals("a.png", result.getOriginalName());
        assertNotNull(result.getFileId());
        verify(fileAssetMapper).insert(any(FileAsset.class));
        verify(objectStorageClient, never()).delete(anyString(), anyString());
    }

    @Test
    void uploadRejectsOwnerOnlyPolicyWithoutOwnerId() {
        when(filePolicyService.getPolicy("cms", "attachment")).thenReturn(ownerOnlyPolicy());
        MockMultipartFile file = new MockMultipartFile("file", "a.bin", "application/octet-stream", new byte[]{1});

        FileSystemException ex = assertThrows(FileSystemException.class,
                () -> fileSystemService.upload(file, "attachment", null, caller()));
        assertEquals(400, ex.getHttpStatus());
        assertEquals("OWNER_ID_REQUIRED", ex.getErrorCode());
        verify(objectStorageClient, never()).put(anyString(), anyString(), any(), anyLong(), anyString());
    }

    @Test
    void uploadPropagatesStoragePutFailure() {
        when(filePolicyService.getPolicy("cms", "article-image")).thenReturn(publicPolicy());
        when(objectStorageClient.put(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenThrow(new IllegalStateException("minio unreachable"));
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});

        assertThrows(RuntimeException.class, () -> fileSystemService.upload(file, "article-image", null, caller()));
        verify(fileAssetMapper, never()).insert(any(FileAsset.class));
    }

    @Test
    void uploadCompensatesDeleteWhenMetadataWriteFails() {
        when(filePolicyService.getPolicy("cms", "article-image")).thenReturn(publicPolicy());
        StoragePutResult putResult = new StoragePutResult("public-assets", "apps/cms/article-image/x.png", "etag-1");
        when(objectStorageClient.put(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn(putResult);
        when(fileAssetMapper.insert(any(FileAsset.class))).thenThrow(new RuntimeException("db down"));
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});

        FileSystemException ex = assertThrows(FileSystemException.class,
                () -> fileSystemService.upload(file, "article-image", null, caller()));
        assertEquals(500, ex.getHttpStatus());
        verify(objectStorageClient).delete("public-assets", "apps/cms/article-image/x.png");
        verify(fileCleanupService, never()).enqueueCleanupTask(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void uploadEnqueuesCleanupTaskWhenCompensatingDeleteAlsoFails() {
        when(filePolicyService.getPolicy("cms", "article-image")).thenReturn(publicPolicy());
        StoragePutResult putResult = new StoragePutResult("public-assets", "apps/cms/article-image/x.png", "etag-1");
        when(objectStorageClient.put(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn(putResult);
        when(fileAssetMapper.insert(any(FileAsset.class))).thenThrow(new RuntimeException("db down"));
        doThrow(new IllegalStateException("delete also failed"))
                .when(objectStorageClient).delete("public-assets", "apps/cms/article-image/x.png");
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});

        assertThrows(FileSystemException.class,
                () -> fileSystemService.upload(file, "article-image", null, caller()));
        verify(fileCleanupService).enqueueCleanupTask(
                anyString(), eq("public-assets"), eq("apps/cms/article-image/x.png"), eq("CLEAN_ORPHAN"), anyString());
    }

    @Test
    void deleteMissingFileThrowsNotFound() {
        when(fileAssetMapper.selectOne(any())).thenReturn(null);
        FileSystemException ex = assertThrows(FileSystemException.class, () -> fileSystemService.delete("missing", caller()));
        assertEquals(404, ex.getHttpStatus());
    }

    @Test
    void deleteIsIdempotentWhenAlreadyDeleted() {
        FileAsset asset = new FileAsset().setFileId("f1").setAppId("cms").setAccessLevel("PUBLIC")
                .setStatus(FileStatus.DELETED.name());
        when(fileAssetMapper.selectOne(any())).thenReturn(asset);

        fileSystemService.delete("f1", caller());

        verify(fileCleanupService, never()).softDelete(any(FileAsset.class));
        verify(fileAuditService).log(any(), eq("f1"), any(), eq("SUCCESS"), any(), any());
    }

    @Test
    void deleteActiveFileTriggersSoftDelete() {
        FileAsset asset = new FileAsset().setFileId("f1").setAppId("cms").setAccessLevel("PUBLIC")
                .setStatus(FileStatus.ACTIVE.name());
        when(fileAssetMapper.selectOne(any())).thenReturn(asset);

        fileSystemService.delete("f1", caller());

        verify(fileCleanupService, times(1)).softDelete(asset);
    }
}
