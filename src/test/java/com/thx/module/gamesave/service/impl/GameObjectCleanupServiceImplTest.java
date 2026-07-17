package com.thx.module.gamesave.service.impl;

import com.thx.module.file.service.FileSystemService;
import com.thx.module.gamesave.config.GameSaveProperties;
import com.thx.module.gamesave.mapper.GameObjectMapper;
import com.thx.module.gamesave.model.GameObject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameObjectCleanupServiceImplTest {

    @Test
    void orphanClaimRechecksThresholdAtomically() {
        GameObjectMapper mapper = mock(GameObjectMapper.class);
        GameSaveProperties properties = new GameSaveProperties();
        properties.setOrphanObjectHours(24);
        properties.setObjectCleanupBatchSize(10);
        GameObject candidate = new GameObject().setObjectId("object-1").setUserId("user-1");
        when(mapper.selectOrphanCandidates(any(Date.class), eq(10)))
                .thenReturn(Collections.singletonList(candidate));
        when(mapper.markOrphanDeleting(eq("object-1"), eq("user-1"), any(Date.class))).thenReturn(1);

        GameObjectCleanupServiceImpl service = new GameObjectCleanupServiceImpl(
                mapper, mock(FileSystemService.class), mock(GameObjectCleanupCompletionService.class), properties);

        assertEquals(1, service.claimOrphans());
        verify(mapper).markOrphanDeleting(eq("object-1"), eq("user-1"), any(Date.class));
    }
}
