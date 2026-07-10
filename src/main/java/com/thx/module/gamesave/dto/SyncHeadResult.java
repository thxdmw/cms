package com.thx.module.gamesave.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 当前游戏云端同步 HEAD。headSnapshotId 为 null 表示尚无任何云端快照。 */
@Data
@AllArgsConstructor
public class SyncHeadResult {
    private String gameId;
    private String headSnapshotId;
    private long version;
}
