package com.thx.common.intercepter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Agent API 认证拦截器
 * 用于验证 Agent 服务调用的 API Key
 *
 * @author tanghaixin
 */
@Slf4j
@Component
public class AgentApiAuthInterceptor implements HandlerInterceptor {

    @Value("${agent.api.key}")
    private String apiKey;

    @Value("${agent.api.enabled}")
    private boolean enabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果未启用认证，直接放行
        if (!enabled) {
            log.debug("Agent API 认证未启用，直接放行");
            return true;
        }

        // 从请求头中获取 API Key
        String requestApiKey = request.getHeader("X-API-Key");
        
        if (requestApiKey == null || requestApiKey.isEmpty()) {
            log.warn("Agent API访问缺少API Key - URI: {}, IP: {}", 
                    request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"缺少API Key，请在请求头中添加 X-API-Key\",\"data\":null}");
            return false;
        }

        // 验证 API Key
        if (!apiKey.equals(requestApiKey)) {
            log.warn("Agent API访问使用无效的API Key - URI: {}, IP: {}", 
                    request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"无效的API Key\",\"data\":null}");
            return false;
        }

        // 验证通过，记录日志
        log.info("Agent API访问验证成功 - URI: {}, IP: {}", 
                request.getRequestURI(), request.getRemoteAddr());
        return true;
    }
}
