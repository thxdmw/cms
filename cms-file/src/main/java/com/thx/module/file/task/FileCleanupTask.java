package com.thx.module.file.task;

import com.thx.module.file.config.FileSystemProperties;
import com.thx.module.file.service.FileCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 文件清理定时任务
 * 定期扫描宽限期已过的逻辑删除文件执行物理清理，并重试到期的清理补偿任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupTask {

    /** 实际执行逻辑删除扫描、物理清理、补偿任务重试 */
    private final FileCleanupService fileCleanupService;
    /** 读取 cleanup.enabled 开关 */
    private final FileSystemProperties fileSystemProperties;

    /** 每 5 分钟扫描一次宽限期已过的 DELETED 文件并物理清理 */
    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void purgeDueFiles() {
        if (!fileSystemProperties.getCleanup().isEnabled()) {
            return;
        }
        try {
            fileCleanupService.purgeDueFiles();
        } catch (Exception e) {
            log.error("执行文件物理清理定时任务失败", e);
        }
    }

    /** 每 5 分钟重试一次到期的 storage_cleanup_task 补偿任务 */
    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 90 * 1000)
    public void retryFailedTasks() {
        if (!fileSystemProperties.getCleanup().isEnabled()) {
            return;
        }
        try {
            fileCleanupService.retryFailedTasks();
        } catch (Exception e) {
            log.error("执行清理补偿任务重试失败", e);
        }
    }
}
