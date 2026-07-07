package com.thx.module.admin.pojo;

import lombok.Data;

import java.util.Objects;

@Data
public class RequestPath {
    private final String method;
    private final String pattern;

    public RequestPath(String method, String pattern) {
        this.method = method;
        this.pattern = pattern;
    }

    public String getMethod() {
        return method;
    }

    public String getPattern() {
        return pattern;
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
