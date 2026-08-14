package com.thx.common.security;

import cn.dev33.satoken.stp.StpUtil;
import com.thx.module.admin.entity.User;
import lombok.experimental.UtilityClass;

/**
 * 当前登录用户的统一访问入口。
 * <p>
 * 项目原先在 16 处直接写 {@code SecurityUtils.getSubject().getPrincipal()} 取当前用户，
 * 把 Shiro 的 API 散布到了各个 Controller/Service 中。迁移到 Sa-Token 时统一收敛到本类，
 * 业务代码只依赖 {@link UserContext}，将来若再更换鉴权框架，只需要改这一个文件。
 * <p>
 * 登录用户对象存放在 Sa-Token 的 Session 中（key 为 {@link #USER_SESSION_KEY}），
 * 由 {@link #login} 写入。Sa-Token 的 Session 已配置为存储到 Redis，因此多实例部署时
 * 各节点都能读到同一份登录信息。
 */
@UtilityClass
public class UserContext {

    /** 登录用户对象在 Sa-Token Session 中的键名。 */
    public static final String USER_SESSION_KEY = "currentUser";

    /**
     * 执行登录：登记登录态并把用户对象写入会话。
     *
     * @param user 已通过密码校验的用户
     */
    public static void login(User user) {
        StpUtil.login(user.getUserId());
        StpUtil.getSession().set(USER_SESSION_KEY, user);
    }

    /**
     * 执行登录并指定是否"记住我"。
     *
     * @param user       已通过密码校验的用户
     * @param rememberMe 为 true 时使用持久化 Cookie，浏览器关闭后登录态仍保留
     */
    public static void login(User user, boolean rememberMe) {
        StpUtil.login(user.getUserId(), rememberMe);
        StpUtil.getSession().set(USER_SESSION_KEY, user);
    }

    /**
     * 获取当前登录用户；未登录时返回 null（不抛异常），与原先 Shiro
     * {@code getSubject().getPrincipal()} 的语义保持一致。
     *
     * @return 当前登录用户，未登录返回 null
     */
    public static User getCurrentUser() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return (User) StpUtil.getSession().get(USER_SESSION_KEY);
    }

    /**
     * 获取当前登录用户的 userId；未登录时返回 null。
     *
     * @return 当前用户 id，未登录返回 null
     */
    public static String getCurrentUserId() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        Object loginId = StpUtil.getLoginIdDefaultNull();
        return loginId == null ? null : loginId.toString();
    }

    /**
     * 用最新的用户对象刷新会话中的缓存，供"修改个人资料"等场景在更新数据库后调用，
     * 避免会话里仍是旧数据。
     *
     * @param user 最新的用户对象
     */
    public static void refreshCurrentUser(User user) {
        if (StpUtil.isLogin()) {
            StpUtil.getSession().set(USER_SESSION_KEY, user);
        }
    }

    /**
     * 当前会话的 token 值，作为"会话 id"用于在线用户列表与踢人操作。
     *
     * @return 当前会话 token
     */
    public static String getCurrentTokenValue() {
        return StpUtil.getTokenValue();
    }

    /** 退出登录，清除当前会话的登录态。 */
    public static void logout() {
        StpUtil.logout();
    }
}
