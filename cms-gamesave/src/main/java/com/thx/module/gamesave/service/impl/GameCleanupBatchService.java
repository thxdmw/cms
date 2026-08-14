package com.thx.module.gamesave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.gamesave.config.GameSaveProperties;
import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameCleanupTaskMapper;
import com.thx.module.gamesave.mapper.GameLibraryMapper;
import com.thx.module.gamesave.mapper.GameSnapshotFileMapper;
import com.thx.module.gamesave.mapper.GameSnapshotMapper;
import com.thx.module.gamesave.model.GameCleanupTask;
import com.thx.module.gamesave.model.GameLibrary;
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
    public boolean process(String taskId, String workerId) {
        GameCleanupTask task = taskMapper.selectOne(new LambdaQueryWrapper<GameCleanupTask>()
                .eq(GameCleanupTask::getTaskId, taskId)
                .eq(GameCleanupTask::getStatus, "RUNNING")
                .eq(GameCleanupTask::getWorkerId, workerId)
                .last("LIMIT 1"));
        if (task == null) {
            throw GameSaveException.conflict("CLEANUP_LEASE_LOST", "游戏清理任务租约已失效");
        }
        if (taskMapper.renewLease(taskId, workerId,
                Math.max(30, properties.getGameCleanupLeaseSeconds())) != 1) {
            throw GameSaveException.conflict("CLEANUP_LEASE_LOST", "游戏清理任务续租失败");
        }

        long cursor = task.getCursor() == null ? 0L : task.getCursor();
        GameSnapshot snapshot = snapshotMapper.selectNextForGameCleanup(
                task.getUserId(), task.getGameId(), cursor);
        if (snapshot == null) {
            int gameUpdated = gameLibraryMapper.completeDeleting(task.getGameId(), task.getUserId());
            if (gameUpdated != 1) {
                GameLibrary current = gameLibraryMapper.selectOwnedIncludingDeleted(task.getGameId(), task.getUserId());
                if (current == null || current.getStatus() == null || current.getStatus() != 0) {
                    throw GameSaveException.conflict("GAME_CLEANUP_STATE_CONFLICT", "游戏删除状态已被并发修改");
                }
            }
            if (taskMapper.complete(taskId, workerId) != 1) {
                throw GameSaveException.conflict("CLEANUP_TASK_STATE_CONFLICT", "清理任务完成状态写入失败");
            }
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
            int deletedFiles = snapshotFileMapper.deleteBatchIds(
                    files.stream().map(GameSnapshotFile::getId).collect(Collectors.toList()));
            if (deletedFiles != files.size()) {
                throw GameSaveException.conflict("SNAPSHOT_FILE_CLEANUP_CONFLICT", "快照文件清理数量发生并发冲突");
            }
        }

        if (files.size() < fileLimit) {
            if (snapshotMapper.markDeleted(snapshot.getSnapshotId(), task.getUserId(), task.getGameId()) != 1) {
                throw GameSaveException.conflict("SNAPSHOT_CLEANUP_STATE_CONFLICT", "快照删除状态发生并发冲突");
            }
            if (taskMapper.advance(taskId, workerId, snapshot.getId()) != 1) {
                throw GameSaveException.conflict("CLEANUP_TASK_STATE_CONFLICT", "清理任务游标推进失败");
            }
        } else {
            if (taskMapper.advance(taskId, workerId, cursor) != 1) {
                throw GameSaveException.conflict("CLEANUP_TASK_STATE_CONFLICT", "清理任务批次状态写入失败");
            }
        }
        return false;
    }
}
