package com.thx.module.gamesave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.gamesave.config.GameSaveProperties;
import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.mapper.GameCleanupTaskMapper;
import com.thx.module.gamesave.mapper.GameLibraryMapper;
import com.thx.module.gamesave.mapper.GameSnapshotFileMapper;
import com.thx.module.gamesave.mapper.GameSnapshotMapper;
import com.thx.module.gamesave.model.GameCleanupTask;
import com.thx.module.gamesave.model.GameSnapshot;
import com.thx.module.gamesave.model.GameSnapshotFile;
import com.thx.module.gamesave.service.GameObjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** 单个清理批次；数据库变更在同一事务内提交，失败时游标和引用释放一起回滚。 */
@Service
@RequiredArgsConstructor
public class GameCleanupBatchService {

    private final GameCleanupTaskMapper taskMapper;
    private final GameLibraryMapper gameLibraryMapper;
    private final GameSnapshotMapper snapshotMapper;
    private final GameSnapshotFileMapper snapshotFileMapper;
    private final GameObjectService objectService;
    private final GameSaveProperties properties;

    @Transactional(rollbackFor = Exception.class)
    public boolean process(String taskId) {
        GameCleanupTask task = taskMapper.selectOne(new LambdaQueryWrapper<GameCleanupTask>()
                .eq(GameCleanupTask::getTaskId, taskId)
                .eq(GameCleanupTask::getStatus, "RUNNING")
                .last("LIMIT 1"));
        if (task == null) {
            return false;
        }

        long cursor = task.getCursor() == null ? 0L : task.getCursor();
        GameSnapshot snapshot = snapshotMapper.selectNextForGameCleanup(
                task.getUserId(), task.getGameId(), cursor);
        if (snapshot == null) {
            gameLibraryMapper.completeDeleting(task.getGameId(), task.getUserId());
            taskMapper.complete(taskId);
            return true;
        }

        int fileLimit = Math.max(1, properties.getGameCleanupFileBatchSize());
        List<GameSnapshotFile> files = snapshotFileMapper.selectCleanupBatch(
                snapshot.getSnapshotId(), fileLimit);
        GameCallerContext caller = new GameCallerContext();
        caller.setUserId(task.getUserId());
        caller.setDeviceId("cleanup-task");
        caller.setIp("127.0.0.1");
        for (GameSnapshotFile file : files) {
            objectService.releaseSnapshotReference(file.getObjectId(), caller);
        }
        if (!files.isEmpty()) {
            snapshotFileMapper.deleteBatchIds(
                    files.stream().map(GameSnapshotFile::getId).collect(Collectors.toList()));
        }

        if (files.size() < fileLimit) {
            snapshotMapper.markDeleted(snapshot.getSnapshotId(), task.getUserId(), task.getGameId());
            taskMapper.advance(taskId, snapshot.getId());
        } else {
            taskMapper.advance(taskId, cursor);
        }
        return false;
    }
}
