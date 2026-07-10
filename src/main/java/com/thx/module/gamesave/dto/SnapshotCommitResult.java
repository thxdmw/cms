package com.thx.module.gamesave.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 快照提交成功结果。 */
@Data
@AllArgsConstructor
public class SnapshotCommitResult {
    private String snapshotId;
    private long headVersion;
    private int fileCount;
    private long logicalSize;
    private int changedFileCount;
}
