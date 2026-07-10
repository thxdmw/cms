package com.thx.module.gamesave.service;

import com.thx.module.gamesave.dto.GameLoginRequest;
import com.thx.module.gamesave.dto.GameLoginResult;
import com.thx.module.gamesave.dto.GameRegisterRequest;

/** GameSave 账号注册、登录与设备 Token 签发服务。 */
public interface GameAuthService {

    /** 创建独立 GameSave 账号并登记当前设备。 */
    GameLoginResult register(GameRegisterRequest request);

    /** 校验账号密码，并为指定设备轮换设备 Token。 */
    GameLoginResult login(GameLoginRequest request);
}
