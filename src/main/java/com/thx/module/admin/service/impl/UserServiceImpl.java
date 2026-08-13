package com.thx.module.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.thx.common.util.CopyUtil;
import com.thx.common.util.CoreConst;
import com.thx.common.security.PasswordService;
import com.thx.common.util.Pagination;
import com.thx.common.util.ResultUtil;
import com.thx.common.util.UUIDUtil;
import com.thx.module.admin.mapper.UserMapper;
import com.thx.module.admin.mapper.UserRoleMapper;
import com.thx.module.admin.entity.User;
import com.thx.module.admin.entity.UserRole;
import com.thx.module.admin.service.UserService;
import com.thx.module.admin.vo.ChangePasswordVo;
import com.thx.module.admin.vo.UserOnlineVo;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import com.thx.common.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.*;

/**
 * {@link UserService} 实现：用户增改查、角色分配、在线用户查询/踢出（基于 Shiro SessionDAO），
 * 以及注册新用户 / 修改密码这两个从 Controller 下沉过来的完整业务流程（见对应方法注释）。
 */
@Service
@AllArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    private final UserMapper userMapper;

    private final UserRoleMapper userRoleMapper;

    private final PasswordService passwordService;


    @Override
    public User selectByUsername(String username) {
        return userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username).eq(User::getStatus, CoreConst.STATUS_VALID));
    }

    @Override
    public int register(User user) {
        return userMapper.insert(user);
    }

    @Override
    public void updateLastLoginTime(User user) {
        Assert.notNull(user, "param: user is null");
        user.setLastLoginTime(new Date());
        userMapper.updateById(user);
    }

    @Override
    public IPage<User> selectUsers(User user, Integer pageNumber, Integer pageSize) {
        IPage<User> page = new Pagination<>(pageNumber, pageSize);
        return userMapper.selectUsers(page, user);
    }

    @Override
    public User selectByUserId(String userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public int updateByUserId(User user) {
        Assert.notNull(user, "param: user is null");
        user.setUpdateTime(new Date());
        return userMapper.updateById(user);
    }

    @Override
    public boolean updateStatusBatch(List<String> userIds, Integer status) {
        return update(Wrappers.<User>lambdaUpdate().in(User::getUserId, userIds)
                .set(User::getStatus, status).set(User::getUpdateTime, new Date()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAssignRole(String userId, List<String> roleIds) {
        userRoleMapper.delete(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId));
        for (String roleId : roleIds) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }

    @Override
    public List<UserOnlineVo> selectOnlineUsers(UserOnlineVo userVo) {
        // Sa-Token 中，每个登录账号对应一个 Account-Session，其 TerminalList 里
        // 每一条代表该账号的一次登录（一个 token / 一个终端）。这里遍历所有账号会话，
        // 再展开各自的终端，即可得到全部在线会话，等价于原先遍历 Shiro SessionDAO 的效果。
        // 被挤下线/踢下线的终端会被 Sa-Token 自动移除，
        // 因此不再需要像原实现那样手工判断 kickout 标记。
        List<UserOnlineVo> onlineUserList = new ArrayList<>();
        List<String> sessionIds = StpUtil.searchSessionId("", 0, -1, false);
        for (String sessionId : sessionIds) {
            SaSession session = StpUtil.getSessionBySessionId(sessionId);
            if (session == null) {
                continue;
            }
            Object userObj = session.get(UserContext.USER_SESSION_KEY);
            if (!(userObj instanceof User user)) {
                continue;
            }
            /*用户名搜索*/
            if (StrUtil.isNotBlank(userVo.getUsername())
                    && (user.getUsername() == null || !user.getUsername().contains(userVo.getUsername()))) {
                continue;
            }
            for (SaTerminalInfo terminal : session.getTerminalList()) {
                onlineUserList.add(toOnlineVo(user, terminal));
            }
        }
        return onlineUserList;
    }

    /**
     * 强制指定会话下线。
     * <p>
     * 参数名沿用历史签名中的 {@code sessionId}，在 Sa-Token 下其实是该次登录的 token 值
     * （由在线用户列表返回）。这里用 kickout 而非 logout，两者都会让对方失去登录态，
     * 区别是 kickout 会让被踢者下次请求时收到"已被踢下线"的明确提示，
     * 与原 Shiro KickoutSessionControlFilter 的语义一致。
     *
     * @param sessionId 目标会话的 token 值
     * @param username  目标用户名，仅用于日志与兼容旧签名
     */
    @Override
    public void kickout(Serializable sessionId, String username) {
        if (sessionId == null) {
            return;
        }
        StpUtil.kickoutByTokenValue(sessionId.toString());
    }

    /**
     * 把 Sa-Token 的会话信息组装成前端需要的在线用户视图对象。
     */
    private UserOnlineVo toOnlineVo(User user, SaTerminalInfo terminal) {
        String tokenValue = terminal.getTokenValue();
        UserOnlineVo userBo = new UserOnlineVo();
        userBo.setUsername(user.getUsername());
        //主机的ip地址
        userBo.setHost(user.getLoginIpAddress());
        //会话标识：Sa-Token 下用 token 值，踢人时按它定位会话
        userBo.setSessionId(tokenValue);
        //最后登录时间
        userBo.setLastLoginTime(user.getLastLoginTime());
        //本次登录（该终端）的创建时间，对应原 Shiro Session 的 startTimestamp
        userBo.setStartTime(new Date(terminal.getCreateTime()));
        // 剩余有效期：Sa-Token 返回秒（-1 表示永不过期），这里统一换算成毫秒以保持前端展示口径不变
        long timeoutSeconds = StpUtil.getStpLogic().getTokenTimeout(tokenValue);
        userBo.setTimeout(timeoutSeconds < 0 ? timeoutSeconds : timeoutSeconds * 1000);
        // 说明：Sa-Token 不记录"最后一次交互时间"（Shiro Session 有 lastAccessTime）。
        // 这里退化为该终端的登录时间，避免前端字段为空；如需精确值需自行在拦截器里维护。
        userBo.setLastAccess(new Date(terminal.getCreateTime()));
        //列表中展示的都是仍然在线的会话
        userBo.setSessionStatus(false);
        return userBo;
    }

    @Override
    public ResponseVo registerNewUser(User userForm, String confirmPassword) {
        String username = userForm.getUsername();
        User existUser = selectByUsername(username);
        if (null != existUser) {
            return ResultUtil.error("用户名已存在");
        }
        String password = userForm.getPassword();
        // 判断两次输入密码是否相等
        if (confirmPassword != null && password != null) {
            if (!confirmPassword.equals(password)) {
                return ResultUtil.error("两次密码不一致");
            }
        }
        userForm.setUserId(UUIDUtil.getUniqueIdByUUId());
        userForm.setStatus(CoreConst.STATUS_VALID);
        Date date = new Date();
        userForm.setCreateTime(date);
        userForm.setUpdateTime(date);
        userForm.setLastLoginTime(date);
        userForm.setPassword(passwordService.encode(userForm.getPassword()));
        int num = register(userForm);
        if (num > 0) {
            return ResultUtil.success("添加用户成功");
        } else {
            return ResultUtil.error("添加用户失败");
        }
    }

    @Override
    public ResponseVo changePassword(ChangePasswordVo changePasswordVo) {
        if (changePasswordVo == null
                || changePasswordVo.getOldPassword() == null
                || changePasswordVo.getNewPassword() == null
                || changePasswordVo.getConfirmNewPassword() == null) {
            return ResultUtil.error("请完整填写密码信息");
        }
        if (!changePasswordVo.getNewPassword().equals(changePasswordVo.getConfirmNewPassword())) {
            return ResultUtil.error("两次密码输入不一致");
        }
        User principal = UserContext.getCurrentUser();
        if (principal == null) {
            return ResultUtil.error("登录状态已失效，请重新登录");
        }
        User loginUser = selectByUserId(principal.getUserId());
        if (loginUser == null) {
            return ResultUtil.error("当前用户不存在");
        }
        // 校验旧密码：passwordService 会自动识别该用户当前是 BCrypt 还是历史 md5 格式
        if (!passwordService.matches(loginUser, changePasswordVo.getOldPassword())) {
            return ResultUtil.error("您输入的旧密码有误");
        }
        User newUser = CopyUtil.getCopy(loginUser, User.class);
        // 新密码一律用 BCrypt 编码（BCrypt 自带随机盐，无需再维护 salt 字段）
        newUser.setPassword(passwordService.encode(changePasswordVo.getNewPassword()));
        updateById(newUser);
        // 改密后强制该账号所有端下线，必须重新用新密码登录。
        // 原实现是清 Shiro 的认证缓存（避免旧密码在缓存过期前仍可用），
        // Sa-Token 不缓存密码，但会话里存着旧的用户快照，直接登出更彻底也更安全。
        StpUtil.logout(loginUser.getUserId());
        return ResultUtil.success("修改密码成功");
    }

}
