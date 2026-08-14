package com.thx.module.gamesave.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** ?????????????????????? GameSave ???????? */
@Getter
@AllArgsConstructor
public class SnapshotManifestFileResult {

    private final String relativePath;
    private final String objectId;
    private final String sha256;
    private final long size;
}
