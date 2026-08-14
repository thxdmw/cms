package com.thx.module.gamesave.service;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.SnapshotCommitRequest;
import com.thx.module.gamesave.dto.SnapshotCommitResult;
import com.thx.module.gamesave.dto.SyncHeadResult;

/** GameSave 不可变快照与同步 HEAD 服务。 */
public interface GameSnapshotService {
    /** 按创建时间倒序读取当前用户指定游戏的快照时间线。 */
    java.util.List<com.thx.module.gamesave.dto.SnapshotSummaryResult> listSnapshots(
            String gameId, int limit, GameCallerContext caller);
    /** 读取当前用户拥有的指定不可变快照及其完整 Manifest。 */
    com.thx.module.gamesave.dto.SnapshotManifestResult getSnapshot(
            String gameId, String snapshotId, GameCallerContext caller);

    /** 获取当前用户指定游戏的云端 HEAD。 */
    SyncHeadResult getHead(String gameId, GameCallerContext caller);

    /** 提交完整 Manifest 并以 CAS 方式推进云端 HEAD。 */
    SnapshotCommitResult commit(String gameId, SnapshotCommitRequest request, GameCallerContext caller);
    /** 删除非当前 HEAD 的历史快照并释放其内容对象引用。 */
    void deleteSnapshot(String gameId, String snapshotId, GameCallerContext caller);
}
