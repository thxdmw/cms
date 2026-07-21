package com.thx.module.gamesave.dto;

import lombok.Data;

import java.util.List;

/** 提交不可变快照请求。expectedHeadSnapshotId 是客户端创建 Manifest 前观察到的云端 HEAD。 */
@Data
public class SnapshotCommitRequest {
    private String expectedHeadSnapshotId;
    private String triggerType;
    private String description;
    private List<SnapshotRootDescriptor> roots;
    private List<SnapshotFileDescriptor> files;
}
