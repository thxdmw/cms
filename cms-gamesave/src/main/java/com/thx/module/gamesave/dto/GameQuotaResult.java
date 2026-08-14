package com.thx.module.gamesave.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 当前用户的物理内容对象配额摘要；重复内容只计算一次。 */
@Getter
@AllArgsConstructor
public class GameQuotaResult {

    private final long quotaBytes;
    private final long usedBytes;
    private final long remainingBytes;
}