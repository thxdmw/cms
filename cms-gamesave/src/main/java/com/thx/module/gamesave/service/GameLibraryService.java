package com.thx.module.gamesave.service;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.GameCreateRequest;
import com.thx.module.gamesave.dto.GameLibraryResult;

import java.util.List;

/** 云端逻辑游戏库服务。 */
public interface GameLibraryService {

    List<GameLibraryResult> list(GameCallerContext caller);

    GameLibraryResult create(GameCreateRequest request, GameCallerContext caller);

    /** 删除游戏、其全部云端快照，并释放不再被任何快照引用的内容对象。 */
    void delete(String gameId, GameCallerContext caller);
}