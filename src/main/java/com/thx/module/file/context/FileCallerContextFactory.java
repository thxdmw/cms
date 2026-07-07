package com.thx.module.file.context;

import com.thx.module.file.enums.CallerType;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * FileCallerContext 构造工具
 * CMS 与文件模块运行在同一个 Spring Boot 进程内，内部调用直接构造
 * SYSTEM 类型的 Caller，不经过 HTTP API Key 认证
 */
public final class FileCallerContextFactory {

    /** CMS 自身固定的 App 标识，对应 file_app.app_id = 'cms' */
    public static final String CMS_APP_ID = "cms";

    /** CMS 作为内部信任调用方，默认拥有全部 Scope（内部 Java 调用不经过 FileAuthInterceptor 的 Scope 校验） */
    private static final Set<String> CMS_SCOPES = new LinkedHashSet<>(
            java.util.Arrays.asList("UPLOAD", "READ", "DELETE", "LIST", "PRESIGN"));

    private FileCallerContextFactory() {
    }

    /**
     * 构造 CMS 内部调用的 Caller 上下文
     * @param userId 当前登录的 CMS 用户 ID，可能为空（如系统任务触发的调用）
     * @param ip     当前请求的客户端 IP，仅用于审计日志，可能为空
     */
    public static FileCallerContext forCms(String userId, String ip) {
        FileCallerContext context = new FileCallerContext();
        context.setAppId(CMS_APP_ID);
        context.setUserId(userId);
        context.setCallerType(CallerType.SYSTEM);
        context.setScopes(CMS_SCOPES);
        context.setRequestId(UUID.randomUUID().toString());
        context.setIp(ip);
        return context;
    }

    /**
     * 构造外部应用通过 API Key 认证后的 Caller 上下文
     */
    public static FileCallerContext forApplication(String appId, Set<String> scopes, String requestId, String ip) {
        FileCallerContext context = new FileCallerContext();
        context.setAppId(appId);
        context.setUserId(null);
        context.setCallerType(CallerType.APPLICATION);
        context.setScopes(scopes);
        context.setRequestId(requestId);
        context.setIp(ip);
        return context;
    }
}
