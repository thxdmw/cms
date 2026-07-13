package com.thx.module.gamesave.service.impl;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.GameRetentionCleanupResult;
import com.thx.module.gamesave.dto.GameRetentionPolicyRequest;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameLibraryMapper;
import com.thx.module.gamesave.mapper.GameSnapshotMapper;
import com.thx.module.gamesave.mapper.GameSyncHeadMapper;
import com.thx.module.gamesave.model.GameLibrary;
import com.thx.module.gamesave.model.GameSnapshot;
import com.thx.module.gamesave.service.GameSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 快照保留数量、当前 HEAD 保护和策略边界测试。 */
@ExtendWith(MockitoExtension.class)
class GameRetentionServiceImplTest {

    @Mock
    private GameLibraryMapper gameLibraryMapper;
    @Mock
    private GameSnapshotMapper gameSnapshotMapper;
    @Mock
    private GameSyncHeadMapper gameSyncHeadMapper;
    @Mock
    private GameSnapshotService gameSnapshotService;

    private GameRetentionServiceImpl service;
    private GameCallerContext caller;

    @BeforeEach
    void setUp() {
        service = new GameRetentionServiceImpl(
                gameLibraryMapper, gameSnapshotMapper, gameSyncHeadMapper, gameSnapshotService);
        caller = new GameCallerContext();
        caller.setUserId("user-1");
        caller.setDeviceId("device-1");
        caller.setIp("127.0.0.1");
    }

    @Test
    void cleanupShouldProtectHeadAndDeleteSnapshotsBeyondCount() {
        GameLibrary game = new GameLibrary()
                .setGameId("game-1").setUserId("user-1")
                .setRetentionEnabled(1).setRetentionCount(2).setRetentionDays(0).setStatus(1);
        GameSnapshot head = snapshot("snapshot-3", 3000L);
        GameSnapshot newestHistory = snapshot("snapshot-2", 2000L);
        GameSnapshot oldestHistory = snapshot("snapshot-1", 1000L);
        when(gameLibraryMapper.selectOwnedForRetention("game-1", "user-1")).thenReturn(game);
        when(gameSyncHeadMapper.selectHeadSnapshotId("user-1", "game-1")).thenReturn("snapshot-3");
        when(gameSnapshotMapper.selectActiveForRetention("user-1", "game-1"))
                .thenReturn(Arrays.asList(head, newestHistory, oldestHistory));

        GameRetentionCleanupResult result = service.cleanup("game-1", caller);

        assertEquals(1, result.getDeletedSnapshotCount());
        verify(gameSnapshotService).deleteSnapshot("game-1", "snapshot-1", caller);
        verify(gameSnapshotService, never()).deleteSnapshot("game-1", "snapshot-3", caller);
        verify(gameSnapshotService, never()).deleteSnapshot("game-1", "snapshot-2", caller);
    }

    @Test
    void invalidRetentionCountShouldBeRejectedBeforeDatabaseWrite() {
        GameRetentionPolicyRequest request = new GameRetentionPolicyRequest();
        request.setEnabled(true);
        request.setRetentionCount(0);
        request.setRetentionDays(0);

        GameSaveException exception = assertThrows(GameSaveException.class,
                () -> service.update("game-1", request, caller));

        assertEquals("INVALID_RETENTION_COUNT", exception.getCode());
        verify(gameLibraryMapper, never()).updateRetentionPolicy(
                "game-1", "user-1", 1, 0, 0);
    }

    private GameSnapshot snapshot(String snapshotId, long timestamp) {
        return new GameSnapshot()
                .setSnapshotId(snapshotId)
                .setUserId("user-1")
                .setGameId("game-1")
                .setStatus("ACTIVE")
                .setCreateTime(new Date(timestamp));
    }
}