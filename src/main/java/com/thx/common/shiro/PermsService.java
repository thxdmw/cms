package com.thx.common.shiro;

import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Component;

/**
 * js调用 thymeleaf 实现按钮权限
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Component("perms")
public class PermsService {
    public boolean hasPerm(String permission) {
        return SecurityUtils.getSubject().isPermitted(permission);
    }
}
