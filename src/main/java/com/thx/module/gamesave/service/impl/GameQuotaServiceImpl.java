package com.thx.module.gamesave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.GameQuotaResult;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameAccountMapper;
import com.thx.module.gamesave.model.GameAccount;
import com.thx.module.gamesave.service.GameQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 使用数据库条件更新实现并发安全的用户配额预占与释放。 */
@Service
@RequiredArgsConstructor
public class GameQuotaServiceImpl implements GameQuotaService {

    private final GameAccountMapper gameAccountMapper;

    @Override
    public GameQuotaResult get(GameCallerContext caller) {
        GameAccount account = gameAccountMapper.selectOne(new LambdaQueryWrapper<GameAccount>()
                .eq(GameAccount::getUserId, caller.getUserId())
                .eq(GameAccount::getStatus, 1)
                .last("LIMIT 1"));
        if (account == null) {
            throw GameSaveException.notFound("ACCOUNT_NOT_FOUND", "GameSave 账户不存在或已停用");
        }
        long quota = account.getQuotaBytes() == null ? Long.MAX_VALUE : account.getQuotaBytes();
        long used = account.getUsedBytes() == null ? 0L : account.getUsedBytes();
        long remaining = quota == Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0L, quota - used);
        return new GameQuotaResult(quota, used, remaining);
    }

    @Override
    public void reserve(String userId, long bytes) {
        validateBytes(bytes);
        if (bytes == 0L) return;
        if (gameAccountMapper.reserveQuota(userId, bytes) != 1) {
            throw GameSaveException.conflict("QUOTA_EXCEEDED", "存储配额不足，无法上传新的存档内容");
        }
    }

    @Override
    public void release(String userId, long bytes) {
        validateBytes(bytes);
        if (bytes == 0L) return;
        if (gameAccountMapper.releaseQuota(userId, bytes) != 1) {
            throw GameSaveException.conflict("QUOTA_STATE_CHANGED", "存储配额状态异常，请稍后重试");
        }
    }

    private void validateBytes(long bytes) {
        if (bytes < 0L) {
            throw GameSaveException.badRequest("INVALID_QUOTA_BYTES", "配额变更字节数不能小于零");
        }
    }
}