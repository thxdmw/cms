package com.thx.module.gamesave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.common.util.UUIDUtil;
import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.GameCreateRequest;
import com.thx.module.gamesave.dto.GameLibraryResult;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameLibraryMapper;
import com.thx.module.gamesave.mapper.GameSnapshotFileMapper;
import com.thx.module.gamesave.mapper.GameSnapshotMapper;
import com.thx.module.gamesave.mapper.GameSyncHeadMapper;
import com.thx.module.gamesave.model.GameLibrary;
import com.thx.module.gamesave.model.GameSnapshot;
import com.thx.module.gamesave.model.GameSnapshotFile;
import com.thx.module.gamesave.service.GameLibraryService;
import com.thx.module.gamesave.service.GameObjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 云端逻辑游戏库默认实现。 */
@Service
@RequiredArgsConstructor
public class GameLibraryServiceImpl implements GameLibraryService {

    private static final Set<String> ALLOWED_PROVIDERS = new HashSet<>(
            Arrays.asList("CUSTOM", "STEAM", "GOG", "EPIC", "OTHER"));
    private static final String ACTIVE = "ACTIVE";

    private final GameLibraryMapper gameLibraryMapper;
    private final GameSnapshotMapper gameSnapshotMapper;
    private final GameSnapshotFileMapper gameSnapshotFileMapper;
    private final GameSyncHeadMapper gameSyncHeadMapper;
    private final GameObjectService gameObjectService;

    @Override
    public List<GameLibraryResult> list(GameCallerContext caller) {
        List<GameLibrary> games = gameLibraryMapper.selectList(new LambdaQueryWrapper<GameLibrary>()
                .eq(GameLibrary::getUserId, caller.getUserId())
                .eq(GameLibrary::getStatus, 1)
                .orderByDesc(GameLibrary::getCreateTime));
        List<GameLibraryResult> results = new ArrayList<>(games.size());
        for (GameLibrary game : games) {
            results.add(GameLibraryResult.from(game));
        }
        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameLibraryResult create(GameCreateRequest request, GameCallerContext caller) {
        validateRequest(request);
        String name = request.getName().trim();
        GameLibrary sameName = gameLibraryMapper.selectActiveByName(caller.getUserId(), name);
        if (sameName != null) {
            throw GameSaveException.conflict("GAME_NAME_EXISTS", "已存在同名游戏，请使用其他名称或先删除原游戏");
        }

        String provider = normalizeProvider(request.getProvider());
        String providerGameId = normalizeNullable(request.getProviderGameId());
        if (!"CUSTOM".equals(provider) && providerGameId == null) {
            throw GameSaveException.badRequest("PROVIDER_GAME_ID_REQUIRED", "非自定义游戏必须提供平台游戏 ID");
        }

        String gameKey = "CUSTOM".equals(provider)
                ? "CUSTOM:" + UUIDUtil.uuid()
                : provider + ":" + providerGameId;
        GameLibrary existing = findByGameKey(caller.getUserId(), gameKey);
        if (existing != null) {
            return GameLibraryResult.from(existing);
        }

        GameLibrary deletedGame = gameLibraryMapper.selectOwnedByNameIncludingDeleted(caller.getUserId(), name);
        if (deletedGame != null && !Integer.valueOf(1).equals(deletedGame.getStatus())) {
            int reactivated = gameLibraryMapper.reactivateDeletedById(
                    deletedGame.getId(), caller.getUserId(), gameKey, provider, providerGameId);
            if (reactivated == 1) {
                deletedGame.setGameKey(gameKey)
                        .setProvider(provider)
                        .setProviderGameId(providerGameId)
                        .setCoverFileId(null)
                        .setRetentionEnabled(0)
                        .setRetentionCount(50)
                        .setRetentionDays(0)
                        .setStatus(1);
                return GameLibraryResult.from(deletedGame);
            }
            GameLibrary concurrentGame = gameLibraryMapper.selectActiveByName(caller.getUserId(), name);
            if (concurrentGame != null) {
                return GameLibraryResult.from(concurrentGame);
            }
            throw GameSaveException.conflict("GAME_STATE_CHANGED", "游戏状态已变化，请重新添加");
        }

        GameLibrary game = new GameLibrary()
                .setGameId(UUIDUtil.uuid())
                .setUserId(caller.getUserId())
                .setGameKey(gameKey)
                .setName(name)
                .setProvider(provider)
                .setProviderGameId(providerGameId)
                .setStatus(1);
        try {
            gameLibraryMapper.insert(game);
            return GameLibraryResult.from(game);
        } catch (DuplicateKeyException duplicate) {
            GameLibrary duplicateName = gameLibraryMapper.selectActiveByName(caller.getUserId(), name);
            if (duplicateName != null) {
                throw GameSaveException.conflict("GAME_NAME_EXISTS", "已存在同名游戏，请使用其他名称或先删除原游戏");
            }
            GameLibrary winner = findByGameKey(caller.getUserId(), gameKey);
            if (winner != null) {
                return GameLibraryResult.from(winner);
            }
            throw duplicate;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String gameId, GameCallerContext caller) {
        if (gameId == null || gameId.trim().isEmpty()) {
            throw GameSaveException.badRequest("INVALID_GAME_ID", "游戏 ID 不能为空");
        }
        String normalizedGameId = gameId.trim();
        GameLibrary game = gameLibraryMapper.selectActiveOwned(normalizedGameId, caller.getUserId());
        if (game == null) {
            throw GameSaveException.notFound("GAME_NOT_FOUND", "游戏不存在或已经删除");
        }

        List<GameSnapshot> snapshots = gameSnapshotMapper.selectList(new LambdaQueryWrapper<GameSnapshot>()
                .eq(GameSnapshot::getUserId, caller.getUserId())
                .eq(GameSnapshot::getGameId, normalizedGameId)
                .eq(GameSnapshot::getStatus, ACTIVE));
        for (GameSnapshot snapshot : snapshots) {
            List<GameSnapshotFile> files = gameSnapshotFileMapper.selectList(new LambdaQueryWrapper<GameSnapshotFile>()
                    .eq(GameSnapshotFile::getSnapshotId, snapshot.getSnapshotId()));
            for (GameSnapshotFile file : files) {
                gameObjectService.releaseSnapshotReference(file.getObjectId(), caller);
            }
            int marked = gameSnapshotMapper.markDeleted(
                    snapshot.getSnapshotId(), caller.getUserId(), normalizedGameId);
            if (marked != 1) {
                throw GameSaveException.conflict("SNAPSHOT_STATE_CHANGED", "删除游戏时快照状态已变化，请重试");
            }
        }

        gameSyncHeadMapper.delete(new LambdaQueryWrapper<com.thx.module.gamesave.model.GameSyncHead>()
                .eq(com.thx.module.gamesave.model.GameSyncHead::getUserId, caller.getUserId())
                .eq(com.thx.module.gamesave.model.GameSyncHead::getGameId, normalizedGameId));
        if (gameLibraryMapper.markDeleted(normalizedGameId, caller.getUserId()) != 1) {
            throw GameSaveException.conflict("GAME_STATE_CHANGED", "游戏状态已变化，请重新加载游戏库");
        }
    }

    private GameLibrary findByGameKey(String userId, String gameKey) {
        return gameLibraryMapper.selectOne(new LambdaQueryWrapper<GameLibrary>()
                .eq(GameLibrary::getUserId, userId)
                .eq(GameLibrary::getGameKey, gameKey)
                .eq(GameLibrary::getStatus, 1)
                .last("LIMIT 1"));
    }

    private void validateRequest(GameCreateRequest request) {
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw GameSaveException.badRequest("INVALID_GAME_NAME", "游戏名称不能为空");
        }
        if (request.getName().trim().length() > 255) {
            throw GameSaveException.badRequest("INVALID_GAME_NAME", "游戏名称长度不能超过 255");
        }
        String provider = normalizeProvider(request.getProvider());
        if (!ALLOWED_PROVIDERS.contains(provider)) {
            throw GameSaveException.badRequest("INVALID_PROVIDER", "不支持的游戏平台类型");
        }
        if (request.getProviderGameId() != null && request.getProviderGameId().trim().length() > 128) {
            throw GameSaveException.badRequest("INVALID_PROVIDER_GAME_ID", "平台游戏 ID 长度不能超过 128");
        }
    }

    private String normalizeProvider(String provider) {
        return provider == null || provider.trim().isEmpty()
                ? "CUSTOM"
                : provider.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}