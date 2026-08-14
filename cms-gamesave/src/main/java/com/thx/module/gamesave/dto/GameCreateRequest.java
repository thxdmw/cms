package com.thx.module.gamesave.dto;

import lombok.Data;

/** 创建云端逻辑游戏请求。 */
@Data
public class GameCreateRequest {
    private String name;
    private String provider;
    private String providerGameId;
}
