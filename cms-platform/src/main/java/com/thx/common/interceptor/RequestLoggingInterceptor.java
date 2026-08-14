package com.thx.common.interceptor;

import com.thx.common.log.HttpAccessLogFilter;
import com.thx.module.admin.entity.User;
import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.interceptor.FileAuthInterceptor;
import com.thx.common.security.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 为访问日志补充 Controller 处理方法和已认证调用方信息。
 *
 * 请求耗时、状态、链路 ID 和正文日志由 {@link HttpAccessLogFilter} 统一处理，
 * 同时覆盖进入 Controller 之前就被拒绝的请求。
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod method = (HandlerMethod) handler;
            request.setAttribute(
                    HttpAccessLogFilter.HANDLER_ATTRIBUTE,
                    method.getBeanType().getSimpleName() + "#" + method.getMethod().getName());
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {

        Object fileCaller = request.getAttribute(FileAuthInterceptor.CALLER_CONTEXT_ATTR);
        if (fileCaller instanceof FileCallerContext) {
            String appId = ((FileCallerContext) fileCaller).getAppId();
            if (appId != null) {
                request.setAttribute(HttpAccessLogFilter.CALLER_ATTRIBUTE, "app:" + appId);
                return;
            }
        }

        try {
            User user = UserContext.getCurrentUser();
            if (user != null && user.getUsername() != null) {
                request.setAttribute(HttpAccessLogFilter.CALLER_ATTRIBUTE, "user:" + user.getUsername());
            }
        } catch (Exception ignored) {
            // 匿名访问时没有登录态，取不到当前用户属正常情况，不影响主请求。
        }
    }
}
