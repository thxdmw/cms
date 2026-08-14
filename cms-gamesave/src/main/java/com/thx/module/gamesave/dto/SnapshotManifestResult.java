package com.thx.module.gamesave.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;
import java.util.List;

/** ????????????????????????????? */
@Getter
@AllArgsConstructor
public class SnapshotManifestResult {

    private final String snapshotId;
    private final String gameId;
    private final String deviceId;
    private final String parentSnapshotId;
    private final String triggerType;
    private final String description;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private final Date createTime;
    private final List<SnapshotRootResult> roots;
    private final List<SnapshotManifestFileResult> files;
}
