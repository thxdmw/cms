package com.thx.module.gamesave.dto;

import lombok.Data;

import java.util.List;

/** 客户端提交的快照存档根目录元数据；pathTemplate 应优先使用可移植环境变量。 */
@Data
public class SnapshotRootDescriptor {
    private String rootId;
    private String rootType;
    private String pathTemplate;
    private String source;
    private Integer confidence;
    private List<String> includePatterns;
    private List<String> excludePatterns;
}
