package com.thx.module.gamesave.dto;

import com.thx.module.gamesave.model.GameLibrary;
import lombok.AllArgsConstructor;
import lombok.Data;

/** 客户端可见的云端逻辑游戏信息。 */
@Data
@AllArgsConstructor
public class GameLibraryResult {
    private String gameId;
    private String name;
    private String provider;
    private String providerGameId;

    public static GameLibraryResult from(GameLibrary game) {
        return new GameLibraryResult(
                game.getGameId(), game.getName(), game.getProvider(), game.getProviderGameId());
    }
}
