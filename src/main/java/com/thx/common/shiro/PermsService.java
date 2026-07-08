package com.thx.common.shiro;

import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Component;

/**
 * 供 Thymeleaf 模板通过 {@code ${@perms.hasPerm('xxx')}} 调用，判断当前登录用户是否拥有
 * 指定权限标识，用于按钮级权限控制（如登录/注册等仍走服务端渲染的页面）。
 * 后台管理 Vue SPA 的按钮权限走的是另一套机制（前端 store 里的 perms 集合），不依赖这个类。
 */
@Component("perms")
public class PermsService {
    public boolean hasPerm(String permission) {
        return SecurityUtils.getSubject().isPermitted(permission);
    }
}
