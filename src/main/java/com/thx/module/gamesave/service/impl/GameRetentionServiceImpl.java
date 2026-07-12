package com.thx.module.gamesave.service.impl;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.GameRetentionCleanupResult;
import com.thx.module.gamesave.dto.GameRetentionPolicyRequest;
import com.thx.module.gamesave.dto.GameRetentionPolicyResult;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameLibraryMapper;
import com.thx.module.gamesave.mapper.GameSnapshotMapper;
import com.thx.module.gamesave.mapper.GameSyncHeadMapper;
import com.thx.module.gamesave.model.GameLibrary;
import com.thx.module.gamesave.model.GameSnapshot;
import com.thx.module.gamesave.service.GameRetentionService;
import com.thx.module.gamesave.service.GameSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/** 保护当前 HEAD 的快照数量与时间双条件保留策略实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameRetentionServiceImpl implements GameRetentionService {

    private static final int MIN_RETENTION_COUNT = 1;
    private static final int MAX_RETENTION_COUNT = 500;
    private static final int MAX_RETENTION_DAYS = 3650;

    private final GameLibraryMapper gameLibraryMapper;
    private final GameSnapshotMapper gameSnapshotMapper;
    private final GameSyncHeadMapper gameSyncHeadMapper;
    private final GameSnapshotService gameSnapshotService;

    @Override
    public GameRetentionPolicyResult get(String gameId, GameCallerContext caller) {
        return GameRetentionPolicyResult.from(requireOwnedGame(gameId, caller.getUserId()));
    }

    @Override
    public GameRetentionPolicyResult update(String gameId,
                                            GameRetentionPolicyRequest request,
                                            GameCallerContext caller) {
        validate(request);
        GameLibrary game = requireOwnedGame(gameId, caller.getUserId());
        int updated = gameLibraryMapper.updateRetentionPolicy(
                game.getGameId(), caller.getUserId(), Boolean.TRUE.equals(request.getEnabled()) ? 1 : 0,
                request.getRetentionCount(), request.getRetentionDays());
        if (updated != 1) {
            throw GameSaveException.conflict("RETENTION_STATE_CHANGED", "游戏保留策略状态已变化，请重新加载");
        }
        game.setRetentionEnabled(Boolean.TRUE.equals(request.getEnabled()) ? 1 : 0)
                .setRetentionCount(request.getRetentionCount())
                .setRetentionDays(request.getRetentionDays());
        return GameRetentionPolicyResult.from(game);
    }

    @Override
    public GameRetentionCleanupResult cleanup(String gameId, GameCallerContext caller) {
        GameLibrary game = requireOwnedGame(gameId, caller.getUserId());
        if (!Integer.valueOf(1).equals(game.getRetentionEnabled())) {
            return new GameRetentionCleanupResult(game.getGameId(), 0);
        }
        return cleanupGame(game, caller);
    }

    @Override
    public int cleanupEnabledGames() {
        int deleted = 0;
        for (GameLibrary game : gameLibraryMapper.selectRetentionEnabledGames()) {
            GameCallerContext caller = new GameCallerContext();
            caller.setUserId(game.getUserId());
            caller.setDeviceId("retention-task");
            caller.setIp("127.0.0.1");
            try {
                deleted += cleanupGame(game, caller).getDeletedSnapshotCount();
            } catch (RuntimeException exception) {
                log.error("GameSave 快照保留任务处理失败: userId={}, gameId={}",
                        game.getUserId(), game.getGameId(), exception);
            }
        }
        return deleted;
    }

    private GameRetentionCleanupResult cleanupGame(GameLibrary game, GameCallerContext caller) {
        int retentionCount = game.getRetentionCount() == null ? 50 : game.getRetentionCount();
        int retentionDays = game.getRetentionDays() == null ? 0 : game.getRetentionDays();
        String headSnapshotId = gameSyncHeadMapper.selectHeadSnapshotId(game.getUserId(), game.getGameId());
        List<GameSnapshot> snapshots = gameSnapshotMapper.selectActiveForRetention(
                game.getUserId(), game.getGameId());
        int keepNonHeadCount = Math.max(0, retentionCount - (headSnapshotId == null ? 0 : 1));
        Date cutoff = retentionDays <= 0
                ? null
                : Date.from(Instant.now().minus(retentionDays, ChronoUnit.DAYS));

        int retainedNonHead = 0;
        int deleted = 0;
        for (GameSnapshot snapshot : snapshots) {
            if (snapshot.getSnapshotId().equals(headSnapshotId)) {
                continue;
            }
            boolean exceedsCount = retainedNonHead >= keepNonHeadCount;
            boolean expired = cutoff != null
                    && snapshot.getCreateTime() != null
                    && snapshot.getCreateTime().before(cutoff);
            if (exceedsCount || expired) {
                try {
                    gameSnapshotService.deleteSnapshot(game.getGameId(), snapshot.getSnapshotId(), caller);
                    deleted++;
                } catch (GameSaveException exception) {
                    log.warn("GameSave 保留任务跳过状态已变化的快照: gameId={}, snapshotId={}, code={}",
                            game.getGameId(), snapshot.getSnapshotId(), exception.getCode());
                }
            } else {
                retainedNonHead++;
            }
        }
        return new GameRetentionCleanupResult(game.getGameId(), deleted);
    }

    private GameLibrary requireOwnedGame(String gameId, String userId) {
        if (gameId == null || gameId.trim().isEmpty()) {
            throw GameSaveException.badRequest("INVALID_GAME_ID", "gameId 不能为空");
        }
        GameLibrary game = gameLibraryMapper.selectOwnedForRetention(gameId.trim(), userId);
        if (game == null) {
            throw GameSaveException.notFound("GAME_NOT_FOUND", "游戏不存在或无权访问");
        }
        return game;
    }

    private void validate(GameRetentionPolicyRequest request) {
        if (request == null || request.getEnabled() == null
                || request.getRetentionCount() == null || request.getRetentionDays() == null) {
            throw GameSaveException.badRequest("INVALID_RETENTION_POLICY", "保留策略字段不能为空");
        }
        if (request.getRetentionCount() < MIN_RETENTION_COUNT
                || request.getRetentionCount() > MAX_RETENTION_COUNT) {
            throw GameSaveException.badRequest("INVALID_RETENTION_COUNT", "保留快照数量必须在 1 到 500 之间");
        }
        if (request.getRetentionDays() < 0 || request.getRetentionDays() > MAX_RETENTION_DAYS) {
            throw GameSaveException.badRequest("INVALID_RETENTION_DAYS", "保留天数必须在 0 到 3650 之间，0 表示不按时间清理");
        }
    }
}