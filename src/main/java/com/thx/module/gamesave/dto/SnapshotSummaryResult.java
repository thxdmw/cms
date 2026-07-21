package com.thx.module.gamesave.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thx.module.gamesave.model.GameSnapshot;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;
import java.util.List;

/** 面向时间线展示的轻量快照信息，不包含完整 Manifest。 */
@Getter
@AllArgsConstructor
public class SnapshotSummaryResult {

    private final String snapshotId;
    private final String parentSnapshotId;
    private final String deviceId;
    private final String triggerType;
    private final String description;
    private final int fileCount;
    private final long logicalSize;
    private final int changedFileCount;
    private final List<SnapshotRootResult> roots;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private final Date createTime;

    public static SnapshotSummaryResult from(GameSnapshot snapshot, List<SnapshotRootResult> roots) {
        return new SnapshotSummaryResult(
                snapshot.getSnapshotId(),
                snapshot.getParentSnapshotId(),
                snapshot.getDeviceId(),
                snapshot.getTriggerType(),
                snapshot.getDescription(),
                snapshot.getFileCount(),
                snapshot.getLogicalSize(),
                snapshot.getChangedFileCount(),
                roots,
                snapshot.getCreateTime());
    }
}
