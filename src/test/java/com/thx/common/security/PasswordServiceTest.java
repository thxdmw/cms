package com.thx.common.security;

import com.thx.module.admin.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 覆盖 {@link PasswordService} 的新旧密码兼容与透明升级流程。
 */
class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService();

    /**
     * 构造一个数据库中仍是历史 md5 密文的老用户。
     * <p>
     * 密文使用固定基准值（admin / salt01 / 123456），来源与
     * {@code PasswordHelperLegacyCompatibilityTest} 相同——按 Shiro SimpleHash 的算法定义
     * 独立计算得出，不依赖已被移除的 Shiro 依赖。
     */
    private static User legacyUser() {
        User user = new User();
        user.setUserId("u-admin");
        user.setUsername("admin");
        user.setSalt("salt01");
        user.setPassword(LEGACY_HASH_OF_123456);
        return user;
    }

    /** admin + salt01 + 明文 123456 在历史 md5 算法下的密文。 */
    private static final String LEGACY_HASH_OF_123456 = "95e11655289545e2cd2a776c0ea5ccc3";

    @Test
    @DisplayName("BCrypt 编码后可校验通过，且每次结果不同（自带随机盐）")
    void encodeThenMatches() {
        String encoded = passwordService.encode("123456");
        String encodedAgain = passwordService.encode("123456");

        assertTrue(encoded.startsWith("$2"), "应为 BCrypt 格式");
        assertNotEquals(encoded, encodedAgain, "BCrypt 自带随机盐，两次编码结果应不同");

        User user = new User();
        user.setPassword(encoded);
        assertTrue(passwordService.matches(user, "123456"));
        assertFalse(passwordService.matches(user, "wrong"));
    }

    @Test
    @DisplayName("历史 md5 用户可以正常登录（不影响存量用户）")
    void legacyUserCanStillLogin() {
        User user = legacyUser();

        assertTrue(passwordService.isLegacyHash(user.getPassword()));
        assertTrue(passwordService.matches(user, "123456"));
        assertFalse(passwordService.matches(user, "wrong"));
    }

    @Test
    @DisplayName("登录成功后透明升级为 BCrypt，且升级后仍能用原密码登录")
    void legacyPasswordIsUpgradedTransparently() {
        User user = legacyUser();
        String originalHash = user.getPassword();

        assertTrue(passwordService.matches(user, "123456"), "升级前应能校验通过");

        boolean upgraded = passwordService.upgradeIfLegacy(user, "123456");

        assertTrue(upgraded, "旧格式用户应被升级");
        assertNotEquals(originalHash, user.getPassword(), "密文应已改变");
        assertTrue(user.getPassword().startsWith("$2"), "升级后应为 BCrypt 格式");
        assertFalse(passwordService.isLegacyHash(user.getPassword()));

        // 关键：升级后用户仍然用同一个明文密码登录，完全无感
        assertTrue(passwordService.matches(user, "123456"), "升级后原密码仍应可用");
        assertFalse(passwordService.matches(user, "wrong"));
    }

    @Test
    @DisplayName("已是 BCrypt 的用户不会被重复升级")
    void bcryptUserIsNotUpgradedAgain() {
        User user = new User();
        user.setUserId("u-1");
        user.setPassword(passwordService.encode("123456"));
        String before = user.getPassword();

        assertFalse(passwordService.upgradeIfLegacy(user, "123456"), "已是新格式，不应升级");
        assertTrue(before.equals(user.getPassword()), "密文不应被改动");
    }

    @Test
    @DisplayName("空值输入判定为不匹配，不抛异常")
    void nullSafety() {
        assertFalse(passwordService.matches(null, "x"));

        User noPassword = new User();
        assertFalse(passwordService.matches(noPassword, "x"));

        User user = new User();
        user.setPassword(passwordService.encode("123456"));
        assertFalse(passwordService.matches(user, null));

        assertFalse(passwordService.upgradeIfLegacy(null, "x"));
    }
}
