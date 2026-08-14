package com.thx.module.gamesave.service;

/** 驱动可重入的游戏删除后台任务。 */
public interface GameCleanupService {

    int cleanupRunnableTasks();
}
