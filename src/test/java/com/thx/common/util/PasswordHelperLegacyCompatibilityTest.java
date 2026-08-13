package com.thx.common.util;

import com.thx.module.admin.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link PasswordHelper} 中用 JDK MessageDigest 重写的历史 md5 算法，
 * 与原先 Shiro {@code SimpleHash("md5", pwd, salt, 2)} 的输出<b>逐字节一致</b>。
 * <p>
 * 这是密码体系迁移中风险最高的一点：如果两者不等价，所有尚未完成透明升级的存量用户
 * 都将无法登录。
 * <p>
 * <b>基准值来源</b>：下面的期望密文是用独立于本项目的实现（Python hashlib）按 Shiro
 * SimpleHash 的算法定义计算得出的——即 {@code md5(md5(salt || password))}，
 * 其中 salt 为 {@code username + "puboot.com" + salt}。
 * 早期版本的本测试直接调用 Shiro 的 SimpleHash 做交叉验证并已通过；
 * Shiro 依赖移除后改为硬编码这些基准值，测试强度不变，且不再依赖任何鉴权框架。
 * <b>这些常量是历史数据的契约，任何情况下都不应"为了让测试通过"而修改。</b>
 */
class PasswordHelperLegacyCompatibilityTest {

    private static User user(String username, String salt, String password) {
        User user = new User();
        user.setUsername(username);
        user.setSalt(salt);
        user.setPassword(password);
        return user;
    }

    @ParameterizedTest(name = "{0}/{1} 的历史密文可被校验通过")
    @CsvSource({
            "admin, salt01,           123456,    95e11655289545e2cd2a776c0ea5ccc3",
            "admin, 3f2a9c1b4e5d6f70, P@ssw0rd!, ac97ad7e64e3da30a354c63252c59a86",
            "alice, samesalt,         123456,    877807399815a8be038f9e5c236127db",
            "bob,   samesalt,         123456,    4003c2a247ab76de50b79e2ab5ff5547"
    })
    @DisplayName("重写的 md5 算法与 Shiro SimpleHash 的历史输出完全一致")
    void rewrittenLegacyHashMatchesHistoricalOutput(String username, String salt,
                                                    String rawPassword, String expectedHash) {
        assertTrue(PasswordHelper.matchesLegacy(user(username, salt, expectedHash), rawPassword),
                "重写实现必须能校验通过 Shiro 时期生成的历史密文");
    }

    @Test
    @DisplayName("盐绑定用户名：同密码同 salt 的不同用户，密文不可互换")
    void saltIsBoundToUsername() {
        String aliceHash = "877807399815a8be038f9e5c236127db";
        String bobHash = "4003c2a247ab76de50b79e2ab5ff5547";

        assertTrue(PasswordHelper.matchesLegacy(user("alice", "samesalt", aliceHash), "123456"));
        assertTrue(PasswordHelper.matchesLegacy(user("bob", "samesalt", bobHash), "123456"));
        // 交叉校验必须失败，确认用户名确实参与了加盐
        assertFalse(PasswordHelper.matchesLegacy(user("alice", "samesalt", bobHash), "123456"));
        assertFalse(PasswordHelper.matchesLegacy(user("bob", "samesalt", aliceHash), "123456"));
    }

    @Test
    @DisplayName("错误密码校验失败")
    void wrongPasswordFails() {
        assertFalse(PasswordHelper.matchesLegacy(
                user("admin", "salt01", "95e11655289545e2cd2a776c0ea5ccc3"), "wrong-password"));
    }

    @Test
    @DisplayName("null 入参不抛异常且判定为不匹配")
    void nullInputsAreRejected() {
        assertFalse(PasswordHelper.matchesLegacy(null, "x"));
        assertFalse(PasswordHelper.matchesLegacy(user("a", "b", null), "x"));
        assertFalse(PasswordHelper.matchesLegacy(user("a", "b", "hash"), null));
    }
}
