package com.thx.module.gamesave.service.impl;

import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.context.FileCallerContextFactory;
import com.thx.module.file.exception.FileSystemException;
import com.thx.module.file.service.FileSystemService;
import com.thx.module.gamesave.config.GameSaveProperties;
import com.thx.module.gamesave.mapper.GameObjectMapper;
import com.thx.module.gamesave.model.GameObject;
import com.thx.module.gamesave.service.GameObjectCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 小批量处理孤儿抢占和文件清理，避免把对象存储副作用放进数据库长事务。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameObjectCleanupServiceImpl implements GameObjectCleanupService {

    private static final String APP_ID = "game-save";
    private static final Set<String> FILE_SCOPES = new LinkedHashSet<>(
            Arrays.asList("UPLOAD", "READ", "DELETE", "LIST", "PRESIGN"));

    private final GameObjectMapper gameObjectMapper;
    private final FileSystemService fileSystemService;
    private final GameObjectCleanupCompletionService completionService;
    private final GameSaveProperties properties;

    @Override
    public int claimOrphans() {
        Date threshold = Date.from(Instant.now().minus(properties.getOrphanObjectHours(), ChronoUnit.HOURS));
        List<GameObject> candidates = gameObjectMapper.selectOrphanCandidates(
                threshold, properties.getObjectCleanupBatchSize());
        int claimed = 0;
        for (GameObject object : candidates) {
            claimed += gameObjectMapper.markOrphanDeleting(object.getObjectId(), object.getUserId(), threshold);
        }
        return claimed;
    }

    @Override
    public int cleanupDeletingObjects() {
        List<GameObject> objects = gameObjectMapper.selectDeletingBatch(properties.getObjectCleanupBatchSize());
        int completed = 0;
        for (GameObject object : objects) {
            try {
                deleteFileIdempotently(object);
                completionService.completeCleanup(object.getObjectId(), object.getUserId(), object.getSize());
                completed++;
            } catch (RuntimeException exception) {
                log.error("GameSave 内容对象清理失败: objectId={}, userId={}, fileId={}",
                        object.getObjectId(), object.getUserId(), object.getFileId(), exception);
            }
        }
        return completed;
    }

    private void deleteFileIdempotently(GameObject object) {
        try {
            fileSystemService.delete(object.getFileId(), fileCaller(object.getUserId()));
        } catch (FileSystemException exception) {
            if (exception.getHttpStatus() != 404) {
                throw exception;
            }
        }
    }

    private FileCallerContext fileCaller(String userId) {
        return FileCallerContextFactory.forInternalApp(APP_ID, userId, FILE_SCOPES, "127.0.0.1");
    }
}
