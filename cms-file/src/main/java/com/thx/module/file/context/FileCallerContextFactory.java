package com.thx.module.file.context;

import com.thx.module.file.enums.CallerType;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * FileCallerContext 构造工具。
 * 内部业务模块应通过 {@link #forInternalApp(String, String, Set, String)} 桥接用户身份，
 * 外部应用 API Key 认证仍使用 {@link #forApplication(String, Set, String, String)}。
 */
public final class FileCallerContextFactory {

    public static final String CMS_APP_ID = "cms";

    private static final Set<String> CMS_SCOPES = new LinkedHashSet<>(
            java.util.Arrays.asList("UPLOAD", "READ", "DELETE", "LIST", "PRESIGN"));

    private FileCallerContextFactory() {
    }

    public static FileCallerContext forCms(String userId, String ip) {
        return forInternalApp(CMS_APP_ID, userId, CMS_SCOPES, ip);
    }

    /**
     * 构造同 JVM 内可信业务模块的 Caller。
     * appId 决定 FileSystem 的应用隔离边界，userId 用于 OWNER_ONLY 权限判断。
     */
    public static FileCallerContext forInternalApp(String appId,
                                                   String userId,
                                                   Set<String> scopes,
                                                   String ip) {
        FileCallerContext context = new FileCallerContext();
        context.setAppId(appId);
        context.setUserId(userId);
        context.setCallerType(CallerType.SYSTEM);
        context.setScopes(new LinkedHashSet<>(scopes));
        context.setRequestId(UUID.randomUUID().toString());
        context.setIp(ip);
        return context;
    }

    /** 构造外部应用通过 API Key 认证后的 Caller；外部 App 不携带用户身份。 */
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
