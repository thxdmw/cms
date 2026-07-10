package com.thx.module.gamesave.service;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.SnapshotCommitRequest;
import com.thx.module.gamesave.dto.SnapshotCommitResult;
import com.thx.module.gamesave.dto.SyncHeadResult;

/** GameSave 不可变快照与同步 HEAD 服务。 */
public interface GameSnapshotService {

    /** 获取当前用户指定游戏的云端 HEAD。 */
    SyncHeadResult getHead(String gameId, GameCallerContext caller);

    /** 提交完整 Manifest 并以 CAS 方式推进云端 HEAD。 */
    SnapshotCommitResult commit(String gameId, SnapshotCommitRequest request, GameCallerContext caller);
}
