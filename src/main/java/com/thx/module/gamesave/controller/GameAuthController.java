package com.thx.module.gamesave.controller;

import com.thx.common.annotation.AnonymousAccess;
import com.thx.module.gamesave.dto.GameLoginRequest;
import com.thx.module.gamesave.dto.GameLoginResult;
import com.thx.module.gamesave.dto.GameRegisterRequest;
import com.thx.module.gamesave.service.GameAuthService;
import com.thx.module.gamesave.vo.GameSaveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** GameSave 客户端账号注册与登录接口。 */
@RestController
@RequestMapping("/api/game-save/v1/auth")
@RequiredArgsConstructor
public class GameAuthController {

    private final GameAuthService gameAuthService;

    /** 创建独立 GameSave 账号，同时签发当前设备 Token。 */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @AnonymousAccess
    public GameSaveResponse<GameLoginResult> register(@RequestBody GameRegisterRequest request) {
        return GameSaveResponse.success("注册成功", gameAuthService.register(request));
    }

    /** 登录成功后轮换指定设备 Token，明文 Token 仅在本次响应返回。 */
    @PostMapping("/login")
    @AnonymousAccess
    public GameSaveResponse<GameLoginResult> login(@RequestBody GameLoginRequest request) {
        return GameSaveResponse.success("登录成功", gameAuthService.login(request));
    }
}
