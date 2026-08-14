package com.thx.module.gamesave.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 单个游戏执行一次保留策略后的清理摘要。 */
@Getter
@AllArgsConstructor
public class GameRetentionCleanupResult {

    private final String gameId;
    private final int deletedSnapshotCount;
}