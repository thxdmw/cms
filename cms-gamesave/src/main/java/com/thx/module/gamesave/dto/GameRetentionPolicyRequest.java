package com.thx.module.gamesave.dto;

import lombok.Data;

/** 用户更新单个游戏快照保留策略的请求。 */
@Data
public class GameRetentionPolicyRequest {

    private Boolean enabled;
    private Integer retentionCount;
    private Integer retentionDays;
}