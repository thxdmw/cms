package com.thx.module.gamesave.service.impl;

import com.thx.module.gamesave.config.GameSaveProperties;
import com.thx.module.gamesave.mapper.GameCleanupTaskMapper;
import com.thx.module.gamesave.model.GameCleanupTask;
import com.thx.module.gamesave.service.GameCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameCleanupServiceImpl implements GameCleanupService {

    private final GameCleanupTaskMapper taskMapper;
    private final GameCleanupBatchService batchService;
    private final GameSaveProperties properties;
    private final String workerId = "gamesave-" + UUID.randomUUID();

    @Override
    public int cleanupRunnableTasks() {
        int limit = Math.max(1, properties.getGameCleanupSnapshotBatchSize());
        List<GameCleanupTask> tasks = taskMapper.selectRunnable(limit);
        int processed = 0;
        for (GameCleanupTask task : tasks) {
            int leaseSeconds = Math.max(30, properties.getGameCleanupLeaseSeconds());
            if (taskMapper.claim(task.getTaskId(), workerId, leaseSeconds) != 1) {
                continue;
            }
            try {
                batchService.process(task.getTaskId(), workerId);
                processed++;
            } catch (RuntimeException failure) {
                if (taskMapper.fail(task.getTaskId(), workerId, safeMessage(failure)) != 1) {
                    log.error("GameSave 游戏清理任务失败状态写入冲突，taskId={}, workerId={}",
                            task.getTaskId(), workerId);
                }
                log.warn("GameSave 游戏清理批次失败，taskId={}", task.getTaskId(), failure);
            }
        }
        return processed;
    }

    private String safeMessage(RuntimeException failure) {
        String message = failure.getMessage();
        return message == null || message.trim().isEmpty()
                ? failure.getClass().getSimpleName()
                : message;
    }
}
