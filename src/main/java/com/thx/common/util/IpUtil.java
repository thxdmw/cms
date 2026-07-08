package com.thx.common.util;

import cn.hutool.extra.servlet.ServletUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;

/**
 * IP 工具类，用于从 HTTP 请求中提取客户端真实 IP。
 */
@Slf4j
@UtilityClass
public class IpUtil {

    /**
     * 获取客户端真实 IP。委托给 Hutool 的 {@link ServletUtil#getClientIP}，
     * 该方法会依次尝试 X-Forwarded-For、Proxy-Client-IP、WL-Proxy-Client-IP 等
     * 常见代理请求头，取不到再回退到 {@link HttpServletRequest#getRemoteAddr()}，
     * 因此在经过 Nginx 等反向代理转发的部署环境下也能拿到真实客户端 IP。
     *
     * @param request 当前 HTTP 请求
     * @return 客户端 IP 地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        return ServletUtil.getClientIP(request);
    }

}
