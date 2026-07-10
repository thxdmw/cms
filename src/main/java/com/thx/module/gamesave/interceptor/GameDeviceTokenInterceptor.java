package com.thx.module.gamesave.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.mapper.GameDeviceMapper;
import com.thx.module.gamesave.model.GameDevice;
import com.thx.module.gamesave.util.GameTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * GameSave 设备 Token 认证拦截器。
 * 只接受 Authorization: Bearer &lt;device-token&gt;，认证成功后把用户/设备身份写入请求属性。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameDeviceTokenInterceptor implements HandlerInterceptor {

    public static final String CALLER_CONTEXT_ATTR = "GAME_SAVE_CALLER_CONTEXT";
    private static final String BEARER_PREFIX = "Bearer ";

    private final GameDeviceMapper gameDeviceMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "缺少设备认证 Token");
            return false;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            writeUnauthorized(response, "设备认证 Token 不能为空");
            return false;
        }

        String tokenHash = GameTokenUtil.sha256Hex(token);
        GameDevice device = gameDeviceMapper.selectOne(new LambdaQueryWrapper<GameDevice>()
                .eq(GameDevice::getTokenHash, tokenHash)
                .eq(GameDevice::getStatus, 1)
                .last("LIMIT 1"));
        if (device == null) {
            log.warn("GameSave 使用无效设备 Token - URI: {}, IP: {}", request.getRequestURI(), request.getRemoteAddr());
            writeUnauthorized(response, "设备认证已失效，请重新登录");
            return false;
        }

        GameCallerContext caller = new GameCallerContext();
        caller.setUserId(device.getUserId());
        caller.setDeviceId(device.getDeviceId());
        caller.setIp(request.getRemoteAddr());
        request.setAttribute(CALLER_CONTEXT_ATTR, caller);
        return true;
    }

    /** 拦截器阶段无法进入 ControllerAdvice，因此直接输出与 GameSave API 一致的 JSON 结构。 */
    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        JSONObject json = new JSONObject();
        json.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        json.put("code", "UNAUTHENTICATED");
        json.put("msg", message);
        json.put("data", null);
        response.getWriter().write(json.toJSONString());
    }
}
