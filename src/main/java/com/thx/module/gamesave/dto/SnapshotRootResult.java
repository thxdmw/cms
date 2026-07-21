package com.thx.module.gamesave.dto;

import com.thx.module.gamesave.model.GameSnapshotRoot;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/** 快照保存时使用的可移植存档根目录描述。 */
@Getter
@AllArgsConstructor
public class SnapshotRootResult {
    private final String rootId;
    private final String rootType;
    private final String pathTemplate;
    private final String source;
    private final int confidence;
    private final List<String> includePatterns;
    private final List<String> excludePatterns;

    public static SnapshotRootResult from(GameSnapshotRoot root,
                                          List<String> includePatterns,
                                          List<String> excludePatterns) {
        return new SnapshotRootResult(
                root.getRootId(),
                root.getRootType(),
                root.getPathTemplate(),
                root.getSource(),
                root.getConfidence(),
                includePatterns == null ? Collections.emptyList() : includePatterns,
                excludePatterns == null ? Collections.emptyList() : excludePatterns);
    }
}
