package com.thx.module.gamesave.dto;

import com.thx.module.gamesave.model.GameLibrary;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 单个云端游戏的快照保留策略。 */
@Getter
@AllArgsConstructor
public class GameRetentionPolicyResult {

    private final String gameId;
    private final boolean enabled;
    private final int retentionCount;
    private final int retentionDays;

    public static GameRetentionPolicyResult from(GameLibrary game) {
        return new GameRetentionPolicyResult(
                game.getGameId(),
                Integer.valueOf(1).equals(game.getRetentionEnabled()),
                game.getRetentionCount() == null ? 50 : game.getRetentionCount(),
                game.getRetentionDays() == null ? 0 : game.getRetentionDays());
    }
}