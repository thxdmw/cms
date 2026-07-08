package com.thx.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.User;
import com.thx.module.admin.vo.ChangePasswordVo;
import com.thx.module.admin.vo.UserOnlineVo;
import com.thx.module.admin.vo.base.ResponseVo;

import java.io.Serializable;
import java.util.List;

/**
 * 系统用户服务，涵盖用户的增删改查、角色分配，以及基于 Shiro + Redis Session 的在线用户查询/踢出。
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查询有效（status=1）用户，常用于登录校验、用户名唯一性校验。
     *
     * @param username 用户名
     * @return 用户；不存在或已失效则返回 null
     */
    User selectByUsername(String username);

    /**
     * 新增用户（不做业务校验，直接插入），如需完整的注册流程请使用 {@link #registerNewUser(User, String)}。
     *
     * @param user 待插入用户（需自行补全 userId、密码加密等字段）
     * @return 影响行数
     */
    int register(User user);

    /**
     * 更新用户的最后登录时间为当前时间。
     *
     * @param user 用户（需携带 userId）
     */
    void updateLastLoginTime(User user);

    /**
     * 分页查询有效用户列表，并关联查出用户所属角色信息，支持按用户名、邮箱、手机号模糊匹配。
     *
     * @param user       查询条件：username、email、phone 模糊匹配
     * @param pageNumber 页码
     * @param pageSize   每页条数
     * @return 分页结果
     */
    IPage<User> selectUsers(User user, Integer pageNumber, Integer pageSize);

    /**
     * 根据用户 id（主键）查询用户。
     *
     * @param userId 用户id
     * @return 用户；不存在则返回 null
     */
    User selectByUserId(String userId);

    /**
     * 根据用户 id 更新用户信息，自动刷新更新时间。
     *
     * @param user 待更新用户（需携带 userId）
     * @return 影响行数
     */
    int updateByUserId(User user);

    /**
     * 批量更新用户状态（如批量启用/禁用），自动刷新更新时间。
     *
     * @param userIds 用户 id 集合
     * @param status  目标状态
     * @return 是否更新成功
     */
    boolean updateStatusBatch(List<String> userIds, Integer status);

    /**
     * 为用户重新分配角色：先清空该用户现有的全部角色关联，再逐条插入新的角色关联。具有事务保护。
     *
     * @param userId  用户id
     * @param roleIds 新的角色 id 列表
     */
    void addAssignRole(String userId, List<String> roleIds);

    /**
     * 查询当前在线用户列表：遍历 Shiro SessionDAO 中的活跃 Session，剔除已被标记踢出（kickout）的会话，
     * 并将 Session 中的登录信息转换为展示用的 VO；支持按用户名做包含匹配过滤。
     *
     * @param userOnlineVo 查询条件：username 不为空时按用户名包含匹配过滤
     * @return 在线用户列表
     */
    List<UserOnlineVo> selectOnlineUsers(UserOnlineVo userOnlineVo);


    /**
     * 强制踢出指定会话：在该 Session 上标记 kickout 属性（使其后续请求被判定为已下线），
     * 并将该会话 id 从 Shiro Redis 缓存中该用户名下的会话队列中移除。
     *
     * @param sessionId 会话 id
     * @param username  会话所属用户名，用于定位 Redis 缓存中的会话队列
     */
    void kickout(Serializable sessionId, String username);

    /**
     * 新增用户：校验用户名是否已存在、两次密码是否一致，通过后补全用户 ID/状态/时间戳并加密密码再保存。
     * 校验失败/保存失败的原因直接承载在返回的 ResponseVo 里，供 Controller 原样返回给前端。
     *
     * @param userForm        表单提交的用户信息（密码为明文）
     * @param confirmPassword 确认密码，用于和 userForm 里的密码比对
     */
    ResponseVo registerNewUser(User userForm, String confirmPassword);

    /**
     * 修改当前登录用户的密码：校验两次新密码一致、旧密码正确后更新密码，并清除该用户的 Shiro 登录缓存
     * （否则旧密码在 Shiro 的认证缓存过期前依然能登录成功）。
     */
    ResponseVo changePassword(ChangePasswordVo changePasswordVo);

}
