package com.thx.module.gamesave.service.impl;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.SnapshotCommitRequest;
import com.thx.module.gamesave.dto.SnapshotCommitResult;
import com.thx.module.gamesave.dto.SnapshotFileDescriptor;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameLibraryMapper;
import com.thx.module.gamesave.mapper.GameObjectMapper;
import com.thx.module.gamesave.mapper.GameSnapshotFileMapper;
import com.thx.module.gamesave.mapper.GameSnapshotMapper;
import com.thx.module.gamesave.mapper.GameSyncHeadMapper;
import com.thx.module.gamesave.model.GameLibrary;
import com.thx.module.gamesave.model.GameObject;
import com.thx.module.gamesave.model.GameSnapshotFile;
import com.thx.module.gamesave.model.GameSnapshot;
import com.thx.module.gamesave.model.GameSyncHead;
import com.thx.module.gamesave.service.GameObjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 不可变快照零变化提交行为测试。 */
@ExtendWith(MockitoExtension.class)
class GameSnapshotServiceImplTest {

    @Mock
    private GameLibraryMapper gameLibraryMapper;
    @Mock
    private GameSnapshotMapper gameSnapshotMapper;
    @Mock
    private GameSnapshotFileMapper gameSnapshotFileMapper;
    @Mock
    private GameSyncHeadMapper gameSyncHeadMapper;
    @Mock
    private GameObjectMapper gameObjectMapper;
    @Mock
    private GameObjectService gameObjectService;

    private GameSnapshotServiceImpl service;
    private GameCallerContext caller;

    @BeforeEach
    void setUp() {
        service = new GameSnapshotServiceImpl(
                gameLibraryMapper,
                gameSnapshotMapper,
                gameSnapshotFileMapper,
                gameSyncHeadMapper,
                gameObjectMapper,
                gameObjectService);

        caller = new GameCallerContext();
        caller.setUserId("user-1");
        caller.setDeviceId("device-001");
        caller.setIp("127.0.0.1");
    }

    @Test
    void unchangedManifestShouldReturnCurrentHeadWithoutCreatingDuplicateSnapshot() {
        String hash = repeat('a', 64);
        long size = 1024L;

        GameLibrary game = new GameLibrary()
                .setGameId("game-1")
                .setUserId("user-1")
                .setStatus(1);
        GameSyncHead head = new GameSyncHead()
                .setUserId("user-1")
                .setGameId("game-1")
                .setHeadSnapshotId("snapshot-100")
                .setVersion(5L);
        GameObject object = new GameObject()
                .setObjectId("object-1")
                .setUserId("user-1")
                .setSha256(hash)
                .setSize(size)
                .setStatus("ACTIVE");
        GameSnapshotFile parentFile = new GameSnapshotFile()
                .setSnapshotId("snapshot-100")
                .setRelativePath("slot/save.dat")
                .setObjectId("object-1")
                .setSha256(hash)
                .setSize(size);

        SnapshotFileDescriptor descriptor = new SnapshotFileDescriptor();
        descriptor.setPath("slot/save.dat");
        descriptor.setSha256(hash);
        descriptor.setSize(size);
        SnapshotCommitRequest request = new SnapshotCommitRequest();
        request.setExpectedHeadSnapshotId("snapshot-100");
        request.setTriggerType("MANUAL");
        request.setDescription("重复同步");
        request.setFiles(Collections.singletonList(descriptor));

        when(gameLibraryMapper.selectOne(any())).thenReturn(game);
        when(gameSyncHeadMapper.selectOne(any())).thenReturn(head);
        when(gameObjectService.requireOwnedObject(eq(hash), eq(size), eq(caller))).thenReturn(object);
        when(gameSnapshotFileMapper.selectList(any())).thenReturn(Collections.singletonList(parentFile));

        SnapshotCommitResult result = service.commit("game-1", request, caller);

        assertEquals("snapshot-100", result.getSnapshotId());
        assertEquals(5L, result.getHeadVersion());
        assertEquals(1, result.getFileCount());
        assertEquals(size, result.getLogicalSize());
        assertEquals(0, result.getChangedFileCount());
        assertFalse(result.isCreated());

        verify(gameSnapshotMapper, never()).insert(any());
        verify(gameSnapshotFileMapper, never()).insert(any());
        verify(gameObjectMapper, never()).incrementReference(anyString(), anyString());
        verify(gameSyncHeadMapper, never()).advanceHeadCas(anyString(), anyString(), any(), anyString());
    }

    @Test
    void deleteHistoricalSnapshotShouldReleaseEveryObjectReferenceAndMarkSnapshotDeleted() {
        GameLibrary game = new GameLibrary().setGameId("game-1").setUserId("user-1").setStatus(1);
        GameSyncHead head = new GameSyncHead()
                .setUserId("user-1").setGameId("game-1").setHeadSnapshotId("snapshot-200").setVersion(2L);
        GameSnapshot snapshot = new GameSnapshot()
                .setSnapshotId("snapshot-100").setUserId("user-1").setGameId("game-1").setStatus("ACTIVE");
        GameSnapshotFile first = new GameSnapshotFile().setSnapshotId("snapshot-100").setObjectId("object-1");
        GameSnapshotFile second = new GameSnapshotFile().setSnapshotId("snapshot-100").setObjectId("object-2");

        when(gameLibraryMapper.selectOne(any())).thenReturn(game);
        when(gameSyncHeadMapper.selectOne(any())).thenReturn(head);
        when(gameSnapshotMapper.selectOne(any())).thenReturn(snapshot);
        when(gameSnapshotFileMapper.selectList(any())).thenReturn(java.util.Arrays.asList(first, second));
        when(gameSnapshotMapper.markDeleted("snapshot-100", "user-1", "game-1")).thenReturn(1);

        service.deleteSnapshot("game-1", "snapshot-100", caller);

        verify(gameObjectService).releaseSnapshotReference("object-1", caller);
        verify(gameObjectService).releaseSnapshotReference("object-2", caller);
        verify(gameSnapshotMapper).markDeleted("snapshot-100", "user-1", "game-1");
    }

    @Test
    void deleteCurrentHeadShouldBeRejectedBeforeReleasingAnyObject() {
        GameLibrary game = new GameLibrary().setGameId("game-1").setUserId("user-1").setStatus(1);
        GameSyncHead head = new GameSyncHead()
                .setUserId("user-1").setGameId("game-1").setHeadSnapshotId("snapshot-100").setVersion(2L);
        when(gameLibraryMapper.selectOne(any())).thenReturn(game);
        when(gameSyncHeadMapper.selectOne(any())).thenReturn(head);

        GameSaveException exception = assertThrows(GameSaveException.class,
                () -> service.deleteSnapshot("game-1", "snapshot-100", caller));

        assertEquals("CANNOT_DELETE_HEAD", exception.getCode());
        verify(gameObjectService, never()).releaseSnapshotReference(anyString(), any());
        verify(gameSnapshotMapper, never()).markDeleted(anyString(), anyString(), anyString());
    }
    private String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            result.append(value);
        }
        return result.toString();
    }
}
