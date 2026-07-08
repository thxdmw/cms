package com.thx.module.file.service.impl;

import com.thx.module.file.config.FileSystemProperties;
import com.thx.module.file.enums.FileStatus;
import com.thx.module.file.mapper.FileAssetMapper;
import com.thx.module.file.mapper.StorageCleanupTaskMapper;
import com.thx.module.file.model.FileAsset;
import com.thx.module.file.model.StorageCleanupTask;
import com.thx.module.file.storage.ObjectStorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FileCleanupServiceImpl 逻辑删除与物理清理测试
 * 重点覆盖 CAS 状态流转（DELETED-&gt;PURGING-&gt;PURGED/PURGE_FAILED）、
 * 并发抢占时的跳过逻辑、物理删除失败后的补偿任务登记与退避重试
 */
@ExtendWith(MockitoExtension.class)
class FileCleanupServiceImplTest {

    @Mock
    private FileAssetMapper fileAssetMapper;
    @Mock
    private StorageCleanupTaskMapper storageCleanupTaskMapper;
    @Mock
    private ObjectStorageClient objectStorageClient;

    private FileSystemProperties fileSystemProperties;
    private FileCleanupServiceImpl fileCleanupService;

    @BeforeEach
    void setUp() {
        fileSystemProperties = new FileSystemProperties();
        fileSystemProperties.getCleanup().setEnabled(true);
        fileSystemProperties.getCleanup().setGraceDays(7);
        fileSystemProperties.getCleanup().setMaxRetryCount(3);
        fileCleanupService = new FileCleanupServiceImpl(fileAssetMapper, storageCleanupTaskMapper, objectStorageClient, fileSystemProperties);
    }

    /** 构造一个宽限期早已过期（deletedAt=epoch）的 DELETED 状态文件 */
    private FileAsset deletedAsset() {
        return new FileAsset().setFileId("f1").setBucket("public-assets").setObjectKey("apps/cms/x.png")
                .setStatus(FileStatus.DELETED.name()).setDeletedAt(new Date(0));
    }

    @Test
    void purgeDueFilesTransitionsThroughPurgingToPurged() {
        when(fileAssetMapper.selectList(any())).thenReturn(Collections.singletonList(deletedAsset()));
        when(fileAssetMapper.update(any(FileAsset.class), any())).thenReturn(1);

        fileCleanupService.purgeDueFiles();

        verify(objectStorageClient).delete("public-assets", "apps/cms/x.png");
        verify(fileAssetMapper, times(2)).update(any(FileAsset.class), any());
        verify(storageCleanupTaskMapper, never()).insert(any());
    }

    @Test
    void purgeOneSkipsWhenCasLoses() {
        when(fileAssetMapper.selectList(any())).thenReturn(Collections.singletonList(deletedAsset()));
        when(fileAssetMapper.update(any(FileAsset.class), any())).thenReturn(0);

        fileCleanupService.purgeDueFiles();

        verify(objectStorageClient, never()).delete(anyString(), anyString());
    }

    @Test
    void purgeOneEnqueuesRetryTaskWhenStorageDeleteFails() {
        when(fileAssetMapper.selectList(any())).thenReturn(Collections.singletonList(deletedAsset()));
        when(fileAssetMapper.update(any(FileAsset.class), any())).thenReturn(1);
        doThrow(new IllegalStateException("network error")).when(objectStorageClient).delete("public-assets", "apps/cms/x.png");

        fileCleanupService.purgeDueFiles();

        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);
        verify(fileAssetMapper, times(2)).update(captor.capture(), any());
        assertEquals(FileStatus.PURGING.name(), captor.getAllValues().get(0).getStatus());
        assertEquals(FileStatus.PURGE_FAILED.name(), captor.getAllValues().get(1).getStatus());

        verify(storageCleanupTaskMapper).insert(any(StorageCleanupTask.class));
    }

    @Test
    void enqueueCleanupTaskInsertsPendingTask() {
        fileCleanupService.enqueueCleanupTask("f1", "public-assets", "apps/cms/x.png", "CLEAN_ORPHAN", "boom");

        ArgumentCaptor<StorageCleanupTask> captor = ArgumentCaptor.forClass(StorageCleanupTask.class);
        verify(storageCleanupTaskMapper).insert(captor.capture());
        StorageCleanupTask task = captor.getValue();
        assertEquals("PENDING", task.getStatus());
        assertEquals(0, task.getRetryCount());
        assertEquals("CLEAN_ORPHAN", task.getTaskType());
    }

    /** 构造一个待重试的 PENDING 补偿任务 */
    private StorageCleanupTask pendingTask() {
        return new StorageCleanupTask().setId(1L).setFileId("f1").setBucket("public-assets")
                .setObjectKey("apps/cms/x.png").setTaskType("DELETE_OBJECT").setStatus("PENDING").setRetryCount(0);
    }

    @Test
    void retryFailedTasksMarksSuccessAndPurgesAsset() {
        when(storageCleanupTaskMapper.selectList(any())).thenReturn(Collections.singletonList(pendingTask()));
        when(storageCleanupTaskMapper.update(any(StorageCleanupTask.class), any())).thenReturn(1);

        fileCleanupService.retryFailedTasks();

        ArgumentCaptor<StorageCleanupTask> captor = ArgumentCaptor.forClass(StorageCleanupTask.class);
        verify(storageCleanupTaskMapper, times(2)).update(captor.capture(), any());
        assertEquals("PROCESSING", captor.getAllValues().get(0).getStatus());
        assertEquals("SUCCESS", captor.getAllValues().get(1).getStatus());

        verify(fileAssetMapper).update(any(FileAsset.class), any());
    }

    @Test
    void retryFailedTasksMarksFailedAfterMaxRetries() {
        StorageCleanupTask task = pendingTask().setRetryCount(2); // maxRetryCount = 3, so next failure hits the cap
        when(storageCleanupTaskMapper.selectList(any())).thenReturn(Collections.singletonList(task));
        when(storageCleanupTaskMapper.update(any(StorageCleanupTask.class), any())).thenReturn(1);
        doThrow(new IllegalStateException("still failing")).when(objectStorageClient).delete("public-assets", "apps/cms/x.png");

        fileCleanupService.retryFailedTasks();

        ArgumentCaptor<StorageCleanupTask> captor = ArgumentCaptor.forClass(StorageCleanupTask.class);
        verify(storageCleanupTaskMapper, times(2)).update(captor.capture(), any());
        assertEquals("FAILED", captor.getAllValues().get(1).getStatus());
        verify(fileAssetMapper, never()).update(any(FileAsset.class), any());
    }

    @Test
    void retryFailedTasksReschedulesBeforeMaxRetries() {
        StorageCleanupTask task = pendingTask().setRetryCount(0);
        when(storageCleanupTaskMapper.selectList(any())).thenReturn(Collections.singletonList(task));
        when(storageCleanupTaskMapper.update(any(StorageCleanupTask.class), any())).thenReturn(1);
        doThrow(new IllegalStateException("temporary failure")).when(objectStorageClient).delete("public-assets", "apps/cms/x.png");

        fileCleanupService.retryFailedTasks();

        ArgumentCaptor<StorageCleanupTask> captor = ArgumentCaptor.forClass(StorageCleanupTask.class);
        verify(storageCleanupTaskMapper, times(2)).update(captor.capture(), any());
        StorageCleanupTask secondUpdate = captor.getAllValues().get(1);
        assertEquals("PENDING", secondUpdate.getStatus());
        assertEquals(1, secondUpdate.getRetryCount());
    }

    @Test
    void softDeleteIsGuardedByCasOnActiveStatus() {
        FileAsset asset = new FileAsset().setFileId("f1");
        when(fileAssetMapper.update(any(FileAsset.class), any())).thenReturn(1);

        fileCleanupService.softDelete(asset);

        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);
        verify(fileAssetMapper).update(captor.capture(), any());
        assertEquals(FileStatus.DELETED.name(), captor.getValue().getStatus());
    }
}
