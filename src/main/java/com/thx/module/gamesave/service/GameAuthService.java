package com.thx.module.gamesave.service;

import com.thx.module.gamesave.dto.GameLoginRequest;
import com.thx.module.gamesave.dto.GameLoginResult;

/** GameSave 账号登录与设备 Token 签发服务。 */
public interface GameAuthService {

    /** 校验账号密码，并为指定设备轮换设备 Token。 */
    GameLoginResult login(GameLoginRequest request);
}
