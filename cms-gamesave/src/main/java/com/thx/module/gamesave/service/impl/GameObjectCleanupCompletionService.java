package com.thx.module.gamesave.service.impl;

import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameObjectMapper;
import com.thx.module.gamesave.service.GameQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 只有 DELETING 到 DELETED 的状态更新成功时才释放配额，确保重试幂等。 */
@Service
@RequiredArgsConstructor
public class GameObjectCleanupCompletionService {

    private final GameObjectMapper gameObjectMapper;
    private final GameQuotaService gameQuotaService;

    @Transactional(rollbackFor = Exception.class)
    public void completeCleanup(String objectId, String userId, long bytes) {
        int updated = gameObjectMapper.markDeletedFromDeleting(objectId, userId);
        if (updated == 0) {
            return;
        }
        if (updated != 1) {
            throw GameSaveException.conflict("OBJECT_CLEANUP_STATE_CHANGED", "内容对象清理状态已变化");
        }
        gameQuotaService.release(userId, bytes);
    }
}
