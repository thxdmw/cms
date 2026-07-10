package com.thx.module.gamesave.service;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.GameCreateRequest;
import com.thx.module.gamesave.dto.GameLibraryResult;

import java.util.List;

/** 云端逻辑游戏库服务。 */
public interface GameLibraryService {

    List<GameLibraryResult> list(GameCallerContext caller);

    GameLibraryResult create(GameCreateRequest request, GameCallerContext caller);
}
