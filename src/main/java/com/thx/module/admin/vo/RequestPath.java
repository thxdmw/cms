package com.thx.module.admin.vo;

import lombok.Data;

import java.util.Objects;

/**
 * 一条"HTTP 方法 + 路径匹配规则"，用于 {@link com.thx.infra.AnonymousPathScanner}
 * 汇总扫描到的 {@code @AnonymousAccess} 免登录路径。
 * equals/hashCode 对 method 做了忽略大小写处理，因为 HTTP 方法名大小写不敏感。
 */
@Data
public class RequestPath {
    private final String method;
    private final String pattern;

    public RequestPath(String method, String pattern) {
        this.method = method;
        this.pattern = pattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequestPath)) return false;
        RequestPath that = (RequestPath) o;
        return method.equalsIgnoreCase(that.method) && pattern.equals(that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method.toLowerCase(), pattern);
    }
}
