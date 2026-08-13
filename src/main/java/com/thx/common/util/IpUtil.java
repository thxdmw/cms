package com.thx.common.util;

import cn.hutool.core.util.StrUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 工具类，用于从 HTTP 请求中提取客户端真实 IP。
 */
@Slf4j
@UtilityClass
public class IpUtil {

    private static final String[] HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    /**
     * 获取客户端真实 IP。依次尝试常见代理转发请求头（X-Forwarded-For、X-Real-IP、
     * Proxy-Client-IP、WL-Proxy-Client-IP 等），取不到再回退到
     * {@link HttpServletRequest#getRemoteAddr()}，
     * 因此在经过 Nginx 等反向代理转发的部署环境下也能拿到真实客户端 IP。
     * 注：X-Forwarded-For 可能为逗号分隔的转发链，取第一个非 unknown 的地址。
     *
     * @param request 当前 HTTP 请求
     * @return 客户端 IP 地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        for (String header : HEADERS) {
            String ip = request.getHeader(header);
            ip = normalize(ip);
            if (StrUtil.isNotBlank(ip)) {
                return ip;
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 规整请求头中的 IP：去空白、跳过 "unknown"，逗号分隔的转发链取首个有效地址。
     */
    private static String normalize(String ip) {
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            return null;
        }
        int commaIndex = ip.indexOf(',');
        if (commaIndex > 0) {
            ip = ip.substring(0, commaIndex);
        }
        ip = ip.trim();
        return (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) ? null : ip;
    }
}