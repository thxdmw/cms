package com.thx.module.gamesave.task;

import com.thx.module.gamesave.service.GameCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 周期执行游戏删除清理；任务状态持久化，因此服务重启后会继续。 */
@Component
@RequiredArgsConstructor
public class GameCleanupScheduler {

    private final GameCleanupService cleanupService;

    @Scheduled(fixedDelayString = "${gamesave.game-cleanup-delay-ms:15000}",
            initialDelayString = "${gamesave.game-cleanup-initial-delay-ms:30000}")
    public void cleanup() {
        cleanupService.cleanupRunnableTasks();
    }
}
