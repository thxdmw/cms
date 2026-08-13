package com.thx.common.security;

import cn.dev33.satoken.stp.StpInterface;
import com.thx.module.admin.service.PermissionService;
import com.thx.module.admin.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 的权限数据源实现，等价于原先 Shiro 中
 * {@code MyShiroRealm.doGetAuthorizationInfo()} 的职责：
 * 告诉框架"某个登录用户拥有哪些权限标识和角色"。
 * <p>
 * Sa-Token 会在需要鉴权时（{@code StpUtil.checkPermission} / {@code @SaCheckPermission} 等）
 * 回调本接口。返回结果由 Sa-Token 自身按会话缓存，无需在这里额外做缓存。
 * <p>
 * 用 {@link Lazy} 注入 Service，避免应用启动早期因 Bean 初始化顺序产生循环依赖。
 */
@Slf4j
@Component
public class SaTokenPermissionImpl implements StpInterface {

    @Lazy
    @Autowired
    private RoleService roleService;

    @Lazy
    @Autowired
    private PermissionService permissionService;

    /**
     * 返回该账号的权限标识集合（对应 Permission 表的 perms 字段）。
     *
     * @param loginId   登录 id，即 User.userId
     * @param loginType 账号体系标识，本项目只有一套，忽略
     * @return 权限标识列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (loginId == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(permissionService.findPermsByUserId(loginId.toString()));
    }

    /**
     * 返回该账号的角色标识集合。
     *
     * @param loginId   登录 id，即 User.userId
     * @param loginType 账号体系标识，本项目只有一套，忽略
     * @return 角色标识列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(roleService.findRoleByUserId(loginId.toString()));
    }
}
