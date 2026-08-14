package com.thx.module.gamesave.task;

import com.thx.module.gamesave.service.GameObjectCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定期抢占过期孤儿并清理 DELETING 对象；每次仅处理受控批次。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameObjectCleanupScheduler {

    private final GameObjectCleanupService cleanupService;

    @Scheduled(fixedDelayString = "${gamesave.object-cleanup-delay-ms:300000}",
            initialDelayString = "${gamesave.object-cleanup-initial-delay-ms:120000}")
    public void cleanup() {
        int claimed = cleanupService.claimOrphans();
        int completed = cleanupService.cleanupDeletingObjects();
        if (claimed > 0 || completed > 0) {
            log.info("GameSave 内容对象清理完成: claimed={}, completed={}", claimed, completed);
        }
    }
}
