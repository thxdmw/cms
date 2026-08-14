package com.thx.module.gamesave.service.impl;

import com.thx.module.gamesave.config.GameSaveProperties;
import com.thx.module.gamesave.mapper.GameCleanupTaskMapper;
import com.thx.module.gamesave.model.GameCleanupTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameCleanupServiceImplTest {

    @Mock private GameCleanupTaskMapper taskMapper;
    @Mock private GameCleanupBatchService batchService;

    @Test
    void expiredRunningTaskShouldBeReclaimedWithLease() {
        GameSaveProperties properties = new GameSaveProperties();
        properties.setGameCleanupSnapshotBatchSize(10);
        properties.setGameCleanupLeaseSeconds(120);
        GameCleanupTask task = new GameCleanupTask().setTaskId("task-expired").setStatus("RUNNING");
        when(taskMapper.selectRunnable(10)).thenReturn(Collections.singletonList(task));
        when(taskMapper.claim(org.mockito.ArgumentMatchers.eq("task-expired"), anyString(),
                org.mockito.ArgumentMatchers.eq(120))).thenReturn(1);

        GameCleanupServiceImpl service = new GameCleanupServiceImpl(taskMapper, batchService, properties);

        assertEquals(1, service.cleanupRunnableTasks());
        ArgumentCaptor<String> worker = ArgumentCaptor.forClass(String.class);
        verify(batchService).process(org.mockito.ArgumentMatchers.eq("task-expired"), worker.capture());
        verify(taskMapper).claim("task-expired", worker.getValue(), 120);
    }
}
