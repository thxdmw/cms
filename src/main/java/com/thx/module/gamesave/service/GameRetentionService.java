package com.thx.module.gamesave.service;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.GameRetentionCleanupResult;
import com.thx.module.gamesave.dto.GameRetentionPolicyRequest;
import com.thx.module.gamesave.dto.GameRetentionPolicyResult;

/** 快照保留策略配置与自动清理服务。 */
public interface GameRetentionService {

    GameRetentionPolicyResult get(String gameId, GameCallerContext caller);

    GameRetentionPolicyResult update(String gameId,
                                     GameRetentionPolicyRequest request,
                                     GameCallerContext caller);

    GameRetentionCleanupResult cleanup(String gameId, GameCallerContext caller);

    int cleanupEnabledGames();
}