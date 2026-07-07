package com.thx.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * HTTP 接口访问日志配置。请求体和响应体可能包含个人信息或安全敏感数据，因此正文日志必须支持按环境配置开关。
 */
@Data
@Component
@ConfigurationProperties(prefix = "cms.http-logging")
public class HttpLoggingProperties {

    private boolean enabled = true;
    private boolean includeRequestBody = false;
    private boolean includeResponseBody = false;
    private int maxPayloadLength = 4096;
    private long slowRequestThresholdMs = 1000L;

    private List<String> excludedPaths = Arrays.asList(
            "/favicon.ico",
            "/webjars/**",
            "/static/**",
            "/assets/**",
            "/libs/**",
            "/css/**",
            "/js/**",
            "/img/**",
            "/blog-app/**",
            "/admin-app/**",
            "/cms/**"
    );

    /**
     * 只记录白名单中的请求头；认证信息和 Cookie 不进入白名单，避免敏感信息泄露。
     */
    private Set<String> includedHeaders = new LinkedHashSet<>(Arrays.asList(
            "accept",
            "content-type",
            "origin",
            "referer",
            "traceparent",
            "user-agent",
            "x-forwarded-for",
            "x-real-ip"
    ));

    private Set<String> sensitiveFields = new LinkedHashSet<>(Arrays.asList(
            "accesskey",
            "accesstoken",
            "apikey",
            "authorization",
            "cookie",
            "credential",
            "password",
            "passwd",
            "pwd",
            "refreshtoken",
            "salt",
            "secret",
            "session",
            "sessionid",
            "token"
    ));
}
