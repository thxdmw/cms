package com.thx.module.gamesave.service.impl;

import com.thx.module.gamesave.config.GameSaveProperties;
import com.thx.module.gamesave.mapper.GameCleanupTaskMapper;
import com.thx.module.gamesave.mapper.GameLibraryMapper;
import com.thx.module.gamesave.mapper.GameSnapshotFileMapper;
import com.thx.module.gamesave.mapper.GameSnapshotMapper;
import com.thx.module.gamesave.model.GameCleanupTask;
import com.thx.module.gamesave.model.GameSnapshot;
import com.thx.module.gamesave.model.GameSnapshotFile;
import com.thx.module.gamesave.service.GameObjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameCleanupBatchServiceTest {

    @Mock private GameCleanupTaskMapper taskMapper;
    @Mock private GameLibraryMapper gameLibraryMapper;
    @Mock private GameSnapshotMapper snapshotMapper;
    @Mock private GameSnapshotFileMapper snapshotFileMapper;
    @Mock private GameObjectService objectService;
    private GameCleanupBatchService service;

    @BeforeEach
    void setUp() {
        GameSaveProperties properties = new GameSaveProperties();
        properties.setGameCleanupFileBatchSize(100);
        properties.setGameCleanupLeaseSeconds(300);
        service = new GameCleanupBatchService(taskMapper, gameLibraryMapper, snapshotMapper,
                snapshotFileMapper, objectService, properties);
    }

    @Test
    void emptyTaskShouldCompleteGameAndTask() {
        when(taskMapper.selectOne(any())).thenReturn(task());
        when(taskMapper.renewLease("task-1", "worker-1", 300)).thenReturn(1);
        when(snapshotMapper.selectNextForGameCleanup("user-1", "game-1", 0L)).thenReturn(null);
        when(gameLibraryMapper.completeDeleting("game-1", "user-1")).thenReturn(1);
        when(taskMapper.complete("task-1", "worker-1")).thenReturn(1);

        assertTrue(service.process("task-1", "worker-1"));

        verify(gameLibraryMapper).completeDeleting("game-1", "user-1");
        verify(taskMapper).complete("task-1", "worker-1");
    }

    @Test
    void cleanupBatchShouldReleaseObjectAndAdvanceCursor() {
        when(taskMapper.selectOne(any())).thenReturn(task());
        when(taskMapper.renewLease("task-1", "worker-1", 300)).thenReturn(1);
        GameSnapshot snapshot = new GameSnapshot().setId(8L).setSnapshotId("snapshot-1");
        when(snapshotMapper.selectNextForGameCleanup("user-1", "game-1", 0L)).thenReturn(snapshot);
        when(snapshotFileMapper.selectCleanupBatch("snapshot-1", 100))
                .thenReturn(Collections.singletonList(
                        new GameSnapshotFile().setId(9L).setObjectId("object-1")));
        when(snapshotFileMapper.deleteBatchIds(Collections.singletonList(9L))).thenReturn(1);
        when(snapshotMapper.markDeleted("snapshot-1", "user-1", "game-1")).thenReturn(1);
        when(taskMapper.advance("task-1", "worker-1", 8L)).thenReturn(1);

        service.process("task-1", "worker-1");

        verify(objectService).releaseSnapshotReference(
                org.mockito.ArgumentMatchers.eq("object-1"), any());
        verify(snapshotFileMapper).deleteBatchIds(Collections.singletonList(9L));
        verify(snapshotMapper).markDeleted("snapshot-1", "user-1", "game-1");
        verify(taskMapper).advance("task-1", "worker-1", 8L);
    }

    private GameCleanupTask task() {
        return new GameCleanupTask()
                .setTaskId("task-1")
                .setUserId("user-1")
                .setGameId("game-1")
                .setStatus("RUNNING")
                .setWorkerId("worker-1")
                .setCursor(0L);
    }
}
