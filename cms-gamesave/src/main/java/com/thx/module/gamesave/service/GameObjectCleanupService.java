package com.thx.module.gamesave.service;

/** 内容对象物理清理入口；任务可重复执行且不会重复释放配额。 */
public interface GameObjectCleanupService {

    int claimOrphans();

    int cleanupDeletingObjects();
}
