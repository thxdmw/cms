package com.thx.common.util;

import cn.hutool.extra.servlet.ServletUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;

/**
 * ip工具类
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Slf4j
@UtilityClass
public class IpUtil {

    public static String getIpAddr(HttpServletRequest request) {
        return ServletUtil.getClientIP(request);
    }

}
