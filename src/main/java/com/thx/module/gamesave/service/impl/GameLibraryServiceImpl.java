package com.thx.module.gamesave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.common.util.UUIDUtil;
import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.GameCreateRequest;
import com.thx.module.gamesave.dto.GameLibraryResult;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameLibraryMapper;
import com.thx.module.gamesave.model.GameLibrary;
import com.thx.module.gamesave.service.GameLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

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

    private final GameLibraryMapper gameLibraryMapper;

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
    public GameLibraryResult create(GameCreateRequest request, GameCallerContext caller) {
        validateRequest(request);
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

        GameLibrary game = new GameLibrary()
                .setGameId(UUIDUtil.uuid())
                .setUserId(caller.getUserId())
                .setGameKey(gameKey)
                .setName(request.getName().trim())
                .setProvider(provider)
                .setProviderGameId(providerGameId)
                .setStatus(1);
        try {
            gameLibraryMapper.insert(game);
            return GameLibraryResult.from(game);
        } catch (DuplicateKeyException duplicate) {
            GameLibrary winner = findByGameKey(caller.getUserId(), gameKey);
            if (winner != null) {
                return GameLibraryResult.from(winner);
            }
            throw duplicate;
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
