package com.thx.module.gamesave.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 登录成功结果。token 明文只在本次响应返回，客户端应立即写入 Windows Credential Manager。 */
@Data
@AllArgsConstructor
public class GameLoginResult {
    private String userId;
    private String deviceId;
    private String deviceToken;
}
