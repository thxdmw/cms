package com.thx.module.file.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.file.annotation.RequiredFileScope;
import com.thx.module.file.config.FileSystemProperties;
import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.context.FileCallerContextFactory;
import com.thx.module.file.mapper.FileAppMapper;
import com.thx.module.file.model.FileApp;
import com.thx.module.file.util.ApiKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;
import java.util.UUID;

/**
 * 文件系统 API Key 认证拦截器
 * App 从数据库 file_app 表读取（而不是配置文件），API Key 只保存 SHA-256 哈希，
 * Scope 校验基于 Controller 方法上的 @RequiredFileScope 注解，未声明一律拒绝（Fail Closed）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileAuthInterceptor implements HandlerInterceptor {

    /** 认证成功后 FileCallerContext 存放的请求属性名，供 Controller 读取 */
    public static final String CALLER_CONTEXT_ATTR = "FILE_CALLER_CONTEXT";

    /** 请求头：调用方 App 标识 */
    private static final String HEADER_APP_ID = "X-File-App-Id";
    /** 请求头：调用方 API Key（明文，只在传输中出现，服务端只保存哈希） */
    private static final String HEADER_API_KEY = "X-File-Api-Key";
    /** 请求头：调用方自带的请求追踪 ID，缺省时服务端自动生成 */
    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    /** HTTP 401：未认证 */
    private static final int SC_UNAUTHORIZED = 401;
    /** HTTP 403：无权限 */
    private static final int SC_FORBIDDEN = 403;

    /** 查询 file_app 表，校验 App 是否存在、是否启用 */
    private final FileAppMapper fileAppMapper;
    /** 读取文件系统模块的启用开关等基础配置 */
    private final FileSystemProperties fileSystemProperties;

    /** 认证与 Scope 校验主流程，详见类注释 */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!fileSystemProperties.isEnabled()) {
            log.debug("文件系统模块未启用，拒绝访问");
            writeError(response, 503, "文件系统当前不可用");
            return false;
        }

        String appId = request.getHeader(HEADER_APP_ID);
        String apiKey = request.getHeader(HEADER_API_KEY);

        if (appId == null || appId.trim().isEmpty()) {
            log.warn("文件系统 API 访问缺少 X-File-App-Id - URI: {}, IP: {}",
                    request.getRequestURI(), request.getRemoteAddr());
            writeError(response, SC_UNAUTHORIZED, "缺少 X-File-App-Id 请求头");
            return false;
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("文件系统 API 访问缺少 X-File-Api-Key - AppId: {}, URI: {}, IP: {}",
                    appId, request.getRequestURI(), request.getRemoteAddr());
            writeError(response, SC_UNAUTHORIZED, "缺少 X-File-Api-Key 请求头");
            return false;
        }

        FileApp app = fileAppMapper.selectOne(
                new LambdaQueryWrapper<FileApp>().eq(FileApp::getAppId, appId.trim()));

        // App 不存在或已禁用统一返回同样的错误信息，避免探测 AppId 是否存在
        if (app == null || app.getStatus() == null || app.getStatus() != 1) {
            log.warn("文件系统 API 访问使用无效或已禁用的 AppId - AppId: {}, URI: {}, IP: {}",
                    appId, request.getRequestURI(), request.getRemoteAddr());
            writeError(response, SC_UNAUTHORIZED, "无效的 AppId");
            return false;
        }

        if (!ApiKeyUtil.matches(apiKey.trim(), app.getApiKeyHash())) {
            log.warn("文件系统 API 访问使用无效的 ApiKey - AppId: {}, URI: {}, IP: {}",
                    appId, request.getRequestURI(), request.getRemoteAddr());
            writeError(response, SC_UNAUTHORIZED, "无效的 ApiKey");
            return false;
        }

        // Scope 校验：必须显式声明 @RequiredFileScope，未声明一律拒绝（Fail Closed）
        if (!(handler instanceof HandlerMethod)) {
            log.warn("文件系统 API 访问的目标不是 Controller 方法，拒绝访问 - URI: {}", request.getRequestURI());
            writeError(response, SC_FORBIDDEN, "拒绝访问");
            return false;
        }
        RequiredFileScope requiredFileScope = ((HandlerMethod) handler).getMethodAnnotation(RequiredFileScope.class);
        if (requiredFileScope == null) {
            log.warn("文件系统 API 接口未声明 @RequiredFileScope，拒绝访问 - AppId: {}, URI: {}", appId, request.getRequestURI());
            writeError(response, SC_FORBIDDEN, "该接口未声明所需权限范围，拒绝访问");
            return false;
        }
        Set<String> allowedScopes = app.scopeSet();
        if (!allowedScopes.contains(requiredFileScope.value())) {
            log.warn("文件系统 API 访问权限不足 - AppId: {}, URI: {}, 需要 Scope: {}, 允许 Scopes: {}",
                    appId, request.getRequestURI(), requiredFileScope.value(), allowedScopes);
            writeError(response, SC_FORBIDDEN, "权限不足，需要 " + requiredFileScope.value() + " 权限");
            return false;
        }

        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        FileCallerContext caller = FileCallerContextFactory.forApplication(
                app.getAppId(), allowedScopes, requestId, request.getRemoteAddr());
        request.setAttribute(CALLER_CONTEXT_ATTR, caller);

        log.debug("文件系统 API 认证成功 - AppId: {}, URI: {}", appId, request.getRequestURI());
        return true;
    }

    /** 直接写 JSON 错误响应（此时请求还未进入 Controller，无法走 FileExceptionHandler） */
    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        JSONObject json = new JSONObject();
        json.put("status", status);
        json.put("msg", message);
        json.put("data", null);
        response.getWriter().write(json.toJSONString());
    }
}
