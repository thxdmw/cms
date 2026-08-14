package com.thx.module.gamesave.dto;

import lombok.Data;

/** 客户端提交的单个快照文件描述。 */
@Data
public class SnapshotFileDescriptor {
    private String path;
    private String sha256;
    private long size;
}
