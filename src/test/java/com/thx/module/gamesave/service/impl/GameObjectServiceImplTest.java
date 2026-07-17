package com.thx.module.gamesave.service.impl;

import com.thx.module.file.service.FileObjectLookupService;
import com.thx.module.file.service.FileSystemService;
import com.thx.module.file.vo.FileInfoResult;
import com.thx.module.file.vo.FileUploadResult;
import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.ObjectDescriptor;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameObjectMapper;
import com.thx.module.gamesave.model.GameObject;
import com.thx.module.gamesave.service.GameQuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** GameSave 内容对象上传补偿边界测试。 */
@ExtendWith(MockitoExtension.class)
class GameObjectServiceImplTest {

    @Mock
    private GameObjectMapper gameObjectMapper;
    @Mock
    private FileSystemService fileSystemService;
    @Mock
    private FileObjectLookupService fileObjectLookupService;
    @Mock
    private GameQuotaService gameQuotaService;

    private GameObjectServiceImpl service;
    private GameCallerContext caller;

    @BeforeEach
    void setUp() {
        service = new GameObjectServiceImpl(gameObjectMapper, fileSystemService, fileObjectLookupService, gameQuotaService);
        caller = new GameCallerContext();
        caller.setUserId("user-1");
        caller.setDeviceId("device-001");
        caller.setIp("127.0.0.1");
    }

    @Test
    void checksumMismatchShouldDeleteJustUploadedFileAsset() throws Exception {
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        String expectedHash = sha256Hex(content);
        MockMultipartFile file = new MockMultipartFile(
                "file", "save.dat", "application/octet-stream", content);

        when(gameObjectMapper.selectAnyByDescriptor("user-1", expectedHash, content.length)).thenReturn(null);
        when(fileObjectLookupService.findActiveByHash(anyString(), anyString(), anyLong(), any()))
                .thenReturn(null);
        when(fileSystemService.upload(eq(file), anyString(), eq("user-1"), any()))
                .thenReturn(new FileUploadResult("file-1", "save.dat", null));
        when(fileSystemService.get(eq("file-1"), any()))
                .thenReturn(fileInfo("file-1", repeat('a', 64), content.length));

        GameSaveException exception = assertThrows(
                GameSaveException.class,
                () -> service.put(file, expectedHash, content.length, caller));

        assertEquals("CHECKSUM_MISMATCH", exception.getCode());
        verify(fileSystemService).delete(eq("file-1"), any());
        verify(gameQuotaService).release("user-1", content.length);
    }

    @Test
    void gameObjectInsertFailureShouldDeleteJustUploadedFileAsset() throws Exception {
        byte[] content = "save-content".getBytes(StandardCharsets.UTF_8);
        String expectedHash = sha256Hex(content);
        MockMultipartFile file = new MockMultipartFile(
                "file", "save.dat", "application/octet-stream", content);
        IllegalStateException databaseFailure = new IllegalStateException("数据库写入失败");

        when(gameObjectMapper.selectAnyByDescriptor("user-1", expectedHash, content.length)).thenReturn(null);
        when(fileObjectLookupService.findActiveByHash(anyString(), anyString(), anyLong(), any()))
                .thenReturn(null);
        when(fileSystemService.upload(eq(file), anyString(), eq("user-1"), any()))
                .thenReturn(new FileUploadResult("file-2", "save.dat", null));
        when(fileSystemService.get(eq("file-2"), any()))
                .thenReturn(fileInfo("file-2", expectedHash, content.length));
        doThrow(databaseFailure).when(gameObjectMapper).insert(any());

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> service.put(file, expectedHash, content.length, caller));

        assertEquals(databaseFailure, actual);
        verify(fileSystemService).delete(eq("file-2"), any());
        verify(gameQuotaService).release("user-1", content.length);
    }

    @Test
    void cleanupFailureShouldNotReplaceOriginalDatabaseException() throws Exception {
        byte[] content = "save-content".getBytes(StandardCharsets.UTF_8);
        String expectedHash = sha256Hex(content);
        MockMultipartFile file = new MockMultipartFile(
                "file", "save.dat", "application/octet-stream", content);
        IllegalStateException databaseFailure = new IllegalStateException("原始数据库异常");

        when(gameObjectMapper.selectAnyByDescriptor("user-1", expectedHash, content.length)).thenReturn(null);
        when(fileObjectLookupService.findActiveByHash(anyString(), anyString(), anyLong(), any()))
                .thenReturn(null);
        when(fileSystemService.upload(eq(file), anyString(), eq("user-1"), any()))
                .thenReturn(new FileUploadResult("file-3", "save.dat", null));
        when(fileSystemService.get(eq("file-3"), any()))
                .thenReturn(fileInfo("file-3", expectedHash, content.length));
        doThrow(databaseFailure).when(gameObjectMapper).insert(any());
        doThrow(new IllegalStateException("补偿删除失败"))
                .when(fileSystemService).delete(eq("file-3"), any());

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> service.put(file, expectedHash, content.length, caller));

        assertEquals(databaseFailure, actual);
    }

    @Test
    void existingObjectShouldNotReserveQuotaAgain() throws Exception {
        byte[] content = "same-content".getBytes(StandardCharsets.UTF_8);
        String hash = sha256Hex(content);
        MockMultipartFile file = new MockMultipartFile(
                "file", "save.dat", "application/octet-stream", content);
        GameObject existing = new GameObject()
                .setId(7L)
                .setObjectId("object-existing")
                .setUserId("user-1")
                .setSha256(hash)
                .setSize((long) content.length)
                .setStatus("ACTIVE");
        when(gameObjectMapper.selectAnyByDescriptor("user-1", hash, content.length)).thenReturn(existing);
        when(gameObjectMapper.touchActiveObject(7L, "user-1")).thenReturn(1);

        GameObject actual = service.put(file, hash, content.length, caller);

        assertEquals(existing, actual);
        verify(gameQuotaService, never()).reserve(anyString(), anyLong());
        verify(fileSystemService, never()).upload(any(), anyString(), anyString(), any());
        verify(gameObjectMapper).touchActiveObject(7L, "user-1");
    }

    @Test
    void findMissingShouldBatchTouchOnlyUnreferencedObjectsAndRecheckState() {
        ObjectDescriptor first = new ObjectDescriptor(repeat('a', 64), 10L);
        ObjectDescriptor second = new ObjectDescriptor(repeat('b', 64), 20L);
        GameObject referenced = new GameObject().setId(1L).setSha256(first.getSha256())
                .setSize(10L).setReferenceCount(2L).setStatus("ACTIVE");
        GameObject unreferenced = new GameObject().setId(2L).setSha256(second.getSha256())
                .setSize(20L).setReferenceCount(0L).setStatus("ACTIVE");
        when(gameObjectMapper.selectActiveByDescriptors(eq("user-1"), any()))
                .thenReturn(java.util.Arrays.asList(referenced, unreferenced))
                .thenReturn(java.util.Arrays.asList(referenced, unreferenced));

        List<ObjectDescriptor> missing = service.findMissing(java.util.Arrays.asList(first, second), caller);

        assertEquals(0, missing.size());
        verify(gameObjectMapper).touchUnreferencedActiveObjects("user-1", java.util.Collections.singletonList(2L));
        verify(gameObjectMapper, never()).touchActiveObject(1L, "user-1");
        verify(gameObjectMapper, never()).touchActiveObject(2L, "user-1");
        verify(gameObjectMapper, org.mockito.Mockito.times(2))
                .selectActiveByDescriptors(eq("user-1"), any());
    }

    @Test
    void findMissingShouldReportObjectLostToConcurrentOrphanClaim() {
        ObjectDescriptor descriptor = new ObjectDescriptor(repeat('c', 64), 30L);
        GameObject candidate = new GameObject().setId(3L).setSha256(descriptor.getSha256())
                .setSize(30L).setReferenceCount(0L).setStatus("ACTIVE");
        when(gameObjectMapper.selectActiveByDescriptors(eq("user-1"), any()))
                .thenReturn(java.util.Collections.singletonList(candidate))
                .thenReturn(java.util.Collections.emptyList());

        List<ObjectDescriptor> missing = service.findMissing(
                java.util.Collections.singletonList(descriptor), caller);

        assertEquals(1, missing.size());
        assertEquals(descriptor.getSha256(), missing.get(0).getSha256());
        verify(gameObjectMapper).touchUnreferencedActiveObjects(
                "user-1", java.util.Collections.singletonList(3L));
    }

    @Test
    void lastSnapshotReferenceShouldEnterDeletingStateWithoutExternalIo() {
        GameObject object = new GameObject()
                .setObjectId("object-1")
                .setUserId("user-1")
                .setFileId("file-1")
                .setSize(4096L)
                .setReferenceCount(0L)
                .setStatus("ACTIVE");
        when(gameObjectMapper.decrementReference("object-1", "user-1")).thenReturn(1);
        when(gameObjectMapper.selectOne(any())).thenReturn(object);
        when(gameObjectMapper.markDeletingIfUnreferenced("object-1", "user-1")).thenReturn(1);

        service.releaseSnapshotReference("object-1", caller);

        verify(gameObjectMapper).markDeletingIfUnreferenced("object-1", "user-1");
        verify(fileSystemService, never()).delete(eq("file-1"), any());
        verify(gameQuotaService, never()).release("user-1", 4096L);
    }
    private FileInfoResult fileInfo(String fileId, String sha256, long size) {
        return new FileInfoResult(
                fileId,
                "save-object",
                "save.dat",
                "dat",
                "application/octet-stream",
                size,
                sha256,
                "OWNER_ONLY",
                "user-1",
                "ACTIVE",
                new Date());
    }

    private String sha256Hex(byte[] bytes) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder result = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

    private String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            result.append(value);
        }
        return result.toString();
    }
}
