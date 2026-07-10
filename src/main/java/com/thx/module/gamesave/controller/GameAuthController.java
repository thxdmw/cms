package com.thx.module.gamesave.controller;

import com.thx.module.gamesave.dto.GameLoginRequest;
import com.thx.module.gamesave.dto.GameLoginResult;
import com.thx.module.gamesave.service.GameAuthService;
import com.thx.module.gamesave.vo.GameSaveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GameSave 客户端账号登录接口。 */
@RestController
@RequestMapping("/api/game-save/v1/auth")
@RequiredArgsConstructor
public class GameAuthController {

    private final GameAuthService gameAuthService;

    /** 登录成功后轮换指定设备 Token，明文 Token 仅在本次响应返回。 */
    @PostMapping("/login")
    public GameSaveResponse<GameLoginResult> login(@RequestBody GameLoginRequest request) {
        return GameSaveResponse.success("登录成功", gameAuthService.login(request));
    }
}
