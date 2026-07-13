package com.thx.module.gamesave.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 快照提交结果；created=false 表示 Manifest 与当前 HEAD 完全一致，本次同步为幂等 no-op。 */
@Data
@AllArgsConstructor
public class SnapshotCommitResult {
    private String snapshotId;
    private long headVersion;
    private int fileCount;
    private long logicalSize;
    private int changedFileCount;
    private boolean created;
}
