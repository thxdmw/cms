package com.thx.common.security;

import com.thx.module.admin.entity.User;
import com.thx.module.admin.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 连接真实数据库，验证历史 md5 用户在登录时被透明升级为 BCrypt 的<b>完整链路</b>，
 * 包括密文确实落库、列宽足够（BCrypt 60 字符不被截断）、升级后仍能用原密码登录。
 * <p>
 * 测试会自行插入并清理一个临时用户，<b>不会触碰任何真实账号</b>。
 */
@SpringBootTest
class PasswordUpgradeIntegrationTest {

    /** 临时测试用户的主键，测试结束后按此清理。 */
    private static final String TEST_USER_ID = "pwd-upgrade-it";
    private static final String TEST_USERNAME = "pwd_upgrade_it_user";
    private static final String TEST_SALT = "it-salt-0001";
    private static final String RAW_PASSWORD = "It@Passw0rd";
    /**
     * 上述用户名/salt/明文在历史 md5 算法下的密文，用于模拟升级前的存量数据。
     * 与 {@code PasswordHelperLegacyCompatibilityTest} 中的基准值同源，独立计算得出。
     */
    private static final String LEGACY_HASH = "0adaae6ea92fd947ad21e88094fdb7d9";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private LoginAuthenticator loginAuthenticator;

    @AfterEach
    void cleanUp() {
        userMapper.deleteById(TEST_USER_ID);
    }

    /** 插入一个密码仍是历史 md5 格式的用户，模拟升级前的存量数据。 */
    private User insertLegacyUser() {
        userMapper.deleteById(TEST_USER_ID);
        User user = new User();
        user.setUserId(TEST_USER_ID);
        user.setUsername(TEST_USERNAME);
        user.setPassword(LEGACY_HASH);
        user.setSalt(TEST_SALT);
        user.setStatus(1);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        userMapper.insert(user);
        return user;
    }

    @Test
    @DisplayName("历史 md5 用户登录后密码被升级为 BCrypt 并正确落库")
    void legacyPasswordUpgradedAndPersisted() {
        User inserted = insertLegacyUser();
        assertTrue(passwordService.isLegacyHash(inserted.getPassword()), "前置条件：应为历史 md5 格式");

        User loaded = userMapper.selectById(TEST_USER_ID);
        assertNotNull(loaded);
        assertTrue(loginAuthenticator.authenticate(loaded, RAW_PASSWORD), "正确密码应校验通过");

        // 关键断言：数据库里的密文已经变成 BCrypt，且没有被列宽截断
        User afterLogin = userMapper.selectById(TEST_USER_ID);
        assertTrue(afterLogin.getPassword().startsWith("$2"), "落库密文应为 BCrypt 格式");
        assertEquals(60, afterLogin.getPassword().length(),
                "BCrypt 哈希必须完整存储为 60 字符，若被截断说明 password 列宽不足");
        assertFalse(passwordService.isLegacyHash(afterLogin.getPassword()));

        // 升级后，用户依旧用同一个明文密码登录
        assertTrue(passwordService.matches(afterLogin, RAW_PASSWORD), "升级后原密码仍应可用");
        assertFalse(passwordService.matches(afterLogin, "wrong-password"));
    }

    @Test
    @DisplayName("密码错误时不校验通过，也不会升级或改动数据库")
    void wrongPasswordDoesNotUpgrade() {
        User inserted = insertLegacyUser();
        String originalHash = inserted.getPassword();

        User loaded = userMapper.selectById(TEST_USER_ID);
        assertFalse(loginAuthenticator.authenticate(loaded, "totally-wrong"), "错误密码不应通过");

        User afterAttempt = userMapper.selectById(TEST_USER_ID);
        assertEquals(originalHash, afterAttempt.getPassword(), "密码错误时不得改动数据库中的密文");
    }

    @Test
    @DisplayName("已升级用户再次登录不会重复写库")
    void alreadyUpgradedUserIsStable() {
        insertLegacyUser();

        // 第一次登录：完成升级
        loginAuthenticator.authenticate(userMapper.selectById(TEST_USER_ID), RAW_PASSWORD);
        String upgradedHash = userMapper.selectById(TEST_USER_ID).getPassword();

        // 第二次登录：应直接走 BCrypt 校验，密文保持不变
        assertTrue(loginAuthenticator.authenticate(userMapper.selectById(TEST_USER_ID), RAW_PASSWORD));

        assertEquals(upgradedHash, userMapper.selectById(TEST_USER_ID).getPassword(),
                "已是 BCrypt 的用户不应被重复改写");
    }
}
