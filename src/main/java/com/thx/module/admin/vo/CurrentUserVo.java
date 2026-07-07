package com.thx.module.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

/**
 * 后台 Vue SPA 用来做客户端权限按钮/菜单展示的当前登录用户信息。
 * perms 直接复用 PermissionService.findPermsByUserId 的结果——和 MyShiroRealm 授权时用的是同一份数据，
 * 不会出现前端以为有权限、后端实际校验失败的不一致。
 *
 * @author tanghaixin
 */
@Data
@AllArgsConstructor
public class CurrentUserVo {
    private String username;
    private String nickname;
    private Set<String> perms;
}
