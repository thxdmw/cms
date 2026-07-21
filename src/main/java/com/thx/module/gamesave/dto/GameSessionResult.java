package com.thx.module.gamesave.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 当前设备 Token 对应的账号与设备身份。 */
@Data
@AllArgsConstructor
public class GameSessionResult {
    private String userId;
    private String deviceId;
    private String username;
}
