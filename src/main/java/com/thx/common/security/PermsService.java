package com.thx.common.security;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * 供 Thymeleaf 模板通过 {@code ${@perms.hasPerm('xxx')}} 调用，判断当前登录用户是否拥有
 * 指定权限标识，用于按钮级权限控制（如登录/注册等仍走服务端渲染的页面）。
 * 后台管理 Vue SPA 的按钮权限走的是另一套机制（前端 store 里的 perms 集合），不依赖这个类。
 * <p>
 * 权限数据来源于 {@link SaTokenPermissionImpl}，与接口鉴权用的是同一份数据。
 */
@Component("perms")
public class PermsService {

    /**
     * 判断当前登录用户是否拥有指定权限。未登录时返回 false（不抛异常），
     * 与原先 Shiro {@code getSubject().isPermitted()} 的行为保持一致。
     *
     * @param permission 权限标识
     * @return 是否拥有该权限
     */
    public boolean hasPerm(String permission) {
        if (!StpUtil.isLogin()) {
            return false;
        }
        return StpUtil.hasPermission(permission);
    }
}
