package com.thx.common.security;

import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.thx.module.admin.entity.User;
import com.thx.module.admin.mapper.UserMapper;
import com.thx.module.admin.service.UserService;
import com.thx.module.admin.vo.UserOnlineVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sa-Token 登录链路的集成测试，覆盖从 Shiro 迁移过来的核心能力，
 * 这些能力无法靠单元测试覆盖，且都是登录体系的关键路径：
 * <ol>
 *   <li>登录后能取回当前用户（替代 {@code SecurityUtils.getSubject().getPrincipal()}）；</li>
 *   <li>登录态与用户快照确实写入了 Redis 会话；</li>
 *   <li>在线用户列表能列出该会话（替代遍历 Shiro SessionDAO）；</li>
 *   <li>踢人后该会话立即失效（替代 KickoutSessionControlFilter）；</li>
 *   <li>登出后登录态清除。</li>
 * </ol>
 * 测试使用 Sa-Token 提供的 Mock 上下文（无真实 HTTP 请求也能操作登录态），
 * 并自行插入/清理临时用户，<b>不触碰任何真实账号</b>。
 */
@SpringBootTest
class SaTokenLoginFlowIntegrationTest {

    private static final String TEST_USER_ID = "sa-flow-it";
    private static final String TEST_USERNAME = "sa_flow_it_user";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @AfterEach
    void cleanUp() {
        // 清理该账号可能残留的登录态与测试用户
        SaTokenContextMockUtil.setMockContext(() -> {
            try {
                StpUtil.logout(TEST_USER_ID);
            } catch (RuntimeException ignored) {
                // 已登出或从未登录，忽略
            }
        });
        userMapper.deleteById(TEST_USER_ID);
    }

    private User insertUser() {
        userMapper.deleteById(TEST_USER_ID);
        User user = new User();
        user.setUserId(TEST_USER_ID);
        user.setUsername(TEST_USERNAME);
        // 该测试只验证会话链路，密码不参与校验，直接存一个 BCrypt 占位值
        user.setPassword("$2a$10$abcdefghijklmnopqrstuvABCDEFGHIJKLMNOPQRSTUVWXYZ012345");
        user.setNickname("集成测试用户");
        user.setLoginIpAddress("127.0.0.1");
        user.setStatus(1);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setLastLoginTime(new Date());
        userMapper.insert(user);
        return user;
    }

    @Test
    @DisplayName("登录后可取回当前用户，登出后登录态清除")
    void loginThenReadCurrentUserThenLogout() {
        User user = insertUser();

        SaTokenContextMockUtil.setMockContext(() -> {
            UserContext.login(user);

            assertTrue(StpUtil.isLogin(), "登录后应处于已登录状态");
            assertEquals(TEST_USER_ID, UserContext.getCurrentUserId());

            User current = UserContext.getCurrentUser();
            assertNotNull(current, "应能从会话取回当前用户");
            assertEquals(TEST_USERNAME, current.getUsername());
            assertEquals("集成测试用户", current.getNickname(), "用户快照应完整存入会话");

            UserContext.logout();
            assertFalse(StpUtil.isLogin(), "登出后不应再处于登录状态");
            assertNull(UserContext.getCurrentUser());
        });
    }

    @Test
    @DisplayName("登录后出现在在线用户列表中，踢人后该会话立即失效")
    void onlineUserListAndKickout() {
        User user = insertUser();

        String tokenValue = SaTokenContextMockUtil.setMockContext(() -> {
            UserContext.login(user);
            return StpUtil.getTokenValue();
        });
        assertNotNull(tokenValue, "登录后应生成 token");

        // 在线用户列表应包含刚登录的这个会话
        UserOnlineVo query = new UserOnlineVo();
        query.setUsername(TEST_USERNAME);
        List<UserOnlineVo> online = userService.selectOnlineUsers(query);

        assertFalse(online.isEmpty(), "在线用户列表应包含刚登录的会话");
        UserOnlineVo vo = online.stream()
                .filter(o -> tokenValue.equals(o.getSessionId()))
                .findFirst()
                .orElse(null);
        assertNotNull(vo, "应能按 token 定位到该在线会话");
        assertEquals(TEST_USERNAME, vo.getUsername());
        assertEquals("127.0.0.1", vo.getHost(), "登录 IP 应随会话一起展示");
        assertNotNull(vo.getStartTime(), "应有会话创建时间");
        assertTrue(vo.getTimeout() != 0, "应能取到剩余有效期");

        // 踢人
        userService.kickout(tokenValue, TEST_USERNAME);

        // 踢人后该 token 不再对应有效登录态
        Object loginIdAfterKickout = StpUtil.getLoginIdByToken(tokenValue);
        assertNull(loginIdAfterKickout, "被踢下线后，token 不应再解析出登录账号");

        List<UserOnlineVo> onlineAfter = userService.selectOnlineUsers(query);
        assertTrue(onlineAfter.stream().noneMatch(o -> tokenValue.equals(o.getSessionId())),
                "被踢下线的会话不应再出现在在线列表中");
    }
}
