package com.thx.module.gamesave.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.gamesave.config.GameSaveProperties;
import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.mapper.GameDeviceMapper;
import com.thx.module.gamesave.model.GameDevice;
import com.thx.module.gamesave.util.GameTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Date;

/** 校验 GameSave 设备 Token，并把用户/设备身份写入请求上下文。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameDeviceTokenInterceptor implements HandlerInterceptor {

    public static final String CALLER_CONTEXT_ATTR = "GAME_SAVE_CALLER_CONTEXT";
    private static final String BEARER_PREFIX = "Bearer ";

    private final GameDeviceMapper gameDeviceMapper;
    private final GameSaveProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String authorization = request.getHeader("Authorization");
        if (authorization == null
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            writeUnauthorized(response, "UNAUTHENTICATED", "缺少设备认证 Token");
            return false;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            writeUnauthorized(response, "UNAUTHENTICATED", "设备认证 Token 不能为空");
            return false;
        }

        GameDevice device = gameDeviceMapper.selectOne(new LambdaQueryWrapper<GameDevice>()
                .eq(GameDevice::getTokenHash, GameTokenUtil.sha256Hex(token))
                .eq(GameDevice::getStatus, 1)
                .last("LIMIT 1"));
        if (device == null) {
            log.warn("GameSave 使用无效设备 Token，uri={}, ip={}, requestId={}",
                    request.getRequestURI(), request.getRemoteAddr(), MDC.get("requestId"));
            writeUnauthorized(response, "UNAUTHENTICATED", "设备认证已失效，请重新登录");
            return false;
        }

        Date now = new Date();
        if (device.getTokenExpireTime() == null || !device.getTokenExpireTime().after(now)) {
            writeUnauthorized(response, "TOKEN_EXPIRED", "设备认证已过期，请重新登录");
            return false;
        }

        GameCallerContext caller = new GameCallerContext();
        caller.setUserId(device.getUserId());
        caller.setDeviceId(device.getDeviceId());
        caller.setIp(request.getRemoteAddr());
        request.setAttribute(CALLER_CONTEXT_ATTR, caller);
        touchLastSeen(device, now);
        return true;
    }

    private void touchLastSeen(GameDevice device, Date now) {
        try {
            Date threshold = new Date(now.getTime()
                    - Duration.ofMinutes(Math.max(1, properties.getLastSeenUpdateMinutes())).toMillis());
            gameDeviceMapper.touchLastSeenIfStale(device.getId(), now, threshold);
        } catch (RuntimeException failure) {
            log.warn("更新 GameSave 设备 lastSeen 失败，deviceId={}, requestId={}",
                    device.getDeviceId(), MDC.get("requestId"), failure);
        }
    }

    private void writeUnauthorized(HttpServletResponse response,
                                   String code,
                                   String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        JSONObject json = new JSONObject();
        json.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        json.put("code", code);
        json.put("msg", message);
        json.put("data", null);
        response.getWriter().write(json.toJSONString());
    }
}
