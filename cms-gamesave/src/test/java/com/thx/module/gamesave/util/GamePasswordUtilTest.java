package com.thx.module.gamesave.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GameSave PBKDF2 密码哈希工具测试。 */
class GamePasswordUtilTest {

    @Test
    void hashPasswordShouldMatchOriginalPassword() {
        String encoded = GamePasswordUtil.hashPassword("correct-password");

        assertTrue(GamePasswordUtil.matches("correct-password", encoded));
        assertFalse(GamePasswordUtil.matches("wrong-password", encoded));
    }

    @Test
    void samePasswordShouldUseDifferentRandomSalt() {
        String first = GamePasswordUtil.hashPassword("same-password");
        String second = GamePasswordUtil.hashPassword("same-password");

        assertNotEquals(first, second);
        assertTrue(GamePasswordUtil.matches("same-password", first));
        assertTrue(GamePasswordUtil.matches("same-password", second));
    }

    @Test
    void malformedHashShouldFailClosed() {
        assertFalse(GamePasswordUtil.matches("password", null));
        assertFalse(GamePasswordUtil.matches("password", "invalid-format"));
        assertFalse(GamePasswordUtil.matches("password", "pbkdf2-sha256$1$bad$bad"));
    }
}
