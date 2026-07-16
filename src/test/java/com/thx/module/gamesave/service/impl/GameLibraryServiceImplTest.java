package com.thx.module.gamesave.service.impl;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.GameCreateRequest;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameLibraryMapper;
import com.thx.module.gamesave.mapper.GameCleanupTaskMapper;
import com.thx.module.gamesave.mapper.GameSnapshotFileMapper;
import com.thx.module.gamesave.mapper.GameSnapshotMapper;
import com.thx.module.gamesave.mapper.GameSyncHeadMapper;
import com.thx.module.gamesave.model.GameLibrary;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 游戏名称唯一约束和删除云端存档回收链路测试。 */
@ExtendWith(MockitoExtension.class)
class GameLibraryServiceImplTest {
    @Mock private GameLibraryMapper gameLibraryMapper;
    @Mock private GameCleanupTaskMapper gameCleanupTaskMapper;
    @Mock private GameSnapshotMapper gameSnapshotMapper;
    @Mock private GameSnapshotFileMapper gameSnapshotFileMapper;
    @Mock private GameSyncHeadMapper gameSyncHeadMapper;
    @Mock private GameObjectService gameObjectService;
    private GameLibraryServiceImpl service;
    private GameCallerContext caller;

    @BeforeEach void setUp() {
        service = new GameLibraryServiceImpl(gameLibraryMapper, gameCleanupTaskMapper,
                gameSnapshotMapper, gameSnapshotFileMapper, gameSyncHeadMapper, gameObjectService);
        caller = new GameCallerContext(); caller.setUserId("user-1"); caller.setDeviceId("device-1");
    }

    @Test void createShouldRejectSameNameForSameAccount() {
        GameCreateRequest request = new GameCreateRequest(); request.setName("Elden Ring"); request.setProvider("CUSTOM");
        when(gameLibraryMapper.selectActiveByName("user-1", "Elden Ring")).thenReturn(new GameLibrary().setGameId("old-game"));
        GameSaveException exception = assertThrows(GameSaveException.class, () -> service.create(request, caller));
        assertEquals("GAME_NAME_EXISTS", exception.getCode());
    }

    @Test void createShouldReactivateDeletedGameWithSameName() {
        GameCreateRequest request = new GameCreateRequest(); request.setName("Local Game"); request.setProvider("CUSTOM");
        GameLibrary deletedGame = new GameLibrary().setId(8L).setGameId("old-game").setUserId("user-1")
                .setName("Local Game").setStatus(0);
        when(gameLibraryMapper.selectOne(any())).thenReturn(null);
        when(gameLibraryMapper.selectOwnedByNameIncludingDeleted("user-1", "Local Game")).thenReturn(deletedGame);
        when(gameLibraryMapper.reactivateDeletedById(org.mockito.ArgumentMatchers.eq(8L), org.mockito.ArgumentMatchers.eq("user-1"), org.mockito.ArgumentMatchers.startsWith("CUSTOM:"), org.mockito.ArgumentMatchers.eq("CUSTOM"), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(1);

        assertEquals("old-game", service.create(request, caller).getGameId());
        assertEquals(Integer.valueOf(1), deletedGame.getStatus());
        assertEquals(Integer.valueOf(50), deletedGame.getRetentionCount());
        assertEquals(Integer.valueOf(0), deletedGame.getRetentionDays());
        verify(gameLibraryMapper, org.mockito.Mockito.never()).insert(any(GameLibrary.class));
    }

    @Test void deleteShouldHideGameAndCreateCleanupTaskWithoutReleasingObjectsInRequest() {
        GameLibrary game = new GameLibrary().setGameId("game-1").setUserId("user-1").setStatus(1);
        when(gameLibraryMapper.selectActiveOwned("game-1", "user-1")).thenReturn(game);
        when(gameLibraryMapper.markDeleting("game-1", "user-1")).thenReturn(1);
        when(gameCleanupTaskMapper.resetForGame(any(), org.mockito.ArgumentMatchers.eq("user-1"),
                org.mockito.ArgumentMatchers.eq("game-1"))).thenReturn(0);

        service.delete("game-1", caller);

        verify(gameLibraryMapper).markDeleting("game-1", "user-1");
        verify(gameSyncHeadMapper).delete(any());
        verify(gameCleanupTaskMapper).insert(any(GameCleanupTask.class));
        verify(gameObjectService, org.mockito.Mockito.never())
                .releaseSnapshotReference(any(), any());
    }
}
