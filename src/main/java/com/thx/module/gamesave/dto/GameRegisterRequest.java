package com.thx.module.gamesave.dto;

import lombok.Data;

/** 客户端账号注册请求，同时登记当前设备。 */
@Data
public class GameRegisterRequest {
    private String username;
    private String password;
    private String deviceId;
    private String deviceName;
}
