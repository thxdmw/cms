package com.thx.module.gamesave.dto;

import lombok.Data;

/** 客户端登录请求。deviceId 由客户端首次启动生成并长期保存。 */
@Data
public class GameLoginRequest {
    private String username;
    private String password;
    private String deviceId;
    private String deviceName;
}
