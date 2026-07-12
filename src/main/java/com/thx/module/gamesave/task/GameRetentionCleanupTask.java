package com.thx.module.gamesave.task;

import com.thx.module.gamesave.service.GameRetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 周期执行用户已显式启用的快照保留策略。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameRetentionCleanupTask {

    private final GameRetentionService gameRetentionService;

    /** 启动五分钟后首次执行，之后每六小时执行一次。 */
    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    public void cleanup() {
        int deleted = gameRetentionService.cleanupEnabledGames();
        if (deleted > 0) {
            log.info("GameSave 快照保留任务完成，删除历史快照数: {}", deleted);
        }
    }
}