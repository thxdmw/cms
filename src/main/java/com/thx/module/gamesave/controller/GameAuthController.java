package com.thx.module.gamesave.controller;

import com.thx.common.annotation.AnonymousAccess;
import com.thx.module.gamesave.dto.GameLoginRequest;
import com.thx.module.gamesave.dto.GameLoginResult;
import com.thx.module.gamesave.dto.GameRegisterRequest;
import com.thx.module.gamesave.config.GameSaveProperties;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.service.GameAuthRateLimitService;
import com.thx.module.gamesave.service.GameAuthService;
import com.thx.module.gamesave.vo.GameSaveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/** GameSave 客户端账号注册与登录接口。 */
@RestController
@RequestMapping("/api/game-save/v1/auth")
@RequiredArgsConstructor
public class GameAuthController {

    private final GameAuthService gameAuthService;
    private final GameAuthRateLimitService rateLimitService;
    private final GameSaveProperties properties;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @AnonymousAccess
    public GameSaveResponse<GameLoginResult> register(@RequestBody GameRegisterRequest request,
                                                       HttpServletRequest servletRequest) {
        rateLimitService.assertAndRecordRegistrationAllowed(clientIp(servletRequest));
        return GameSaveResponse.success("注册成功", gameAuthService.register(request));
    }

    @PostMapping("/login")
    @AnonymousAccess
    public GameSaveResponse<GameLoginResult> login(@RequestBody GameLoginRequest request,
                                                    HttpServletRequest servletRequest) {
        String username = request == null ? null : request.getUsername();
        String ip = clientIp(servletRequest);
        rateLimitService.assertLoginAllowed(username, ip);
        try {
            GameLoginResult result = gameAuthService.login(request);
            rateLimitService.recordLoginSuccess(username, ip);
            return GameSaveResponse.success("登录成功", result);
        } catch (GameSaveException failure) {
            if ("INVALID_CREDENTIALS".equals(failure.getCode())) {
                rateLimitService.recordLoginFailure(username, ip);
            }
            throw failure;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String remoteAddress = normalizeIp(request.getRemoteAddr(), "unknown");
        boolean trustedProxy = properties.getTrustedProxyAddresses().stream()
                .map(String::trim)
                .anyMatch(remoteAddress::equalsIgnoreCase);
        String forwarded = trustedProxy ? request.getHeader("X-Forwarded-For") : null;
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            return normalizeIp(forwarded.split(",", 2)[0], remoteAddress);
        }
        return remoteAddress;
    }

    private String normalizeIp(String value, String fallback) {
        if (value == null) return fallback;
        String normalized = value.trim();
        return normalized.length() >= 3 && normalized.length() <= 45
                && normalized.matches("[0-9a-fA-F:.]+") ? normalized : fallback;
    }
}
