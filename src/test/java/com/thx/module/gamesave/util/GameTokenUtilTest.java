package com.thx.module.gamesave.util;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GameSave 设备 Token 生成与哈希测试。 */
class GameTokenUtilTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("^gs_[A-Za-z0-9_-]{43}$");

    @Test
    void generateTokenShouldReturnHighEntropyUrlSafeToken() {
        String token = GameTokenUtil.generateToken();

        assertTrue(TOKEN_PATTERN.matcher(token).matches());
    }

    @Test
    void consecutiveTokensShouldBeDifferent() {
        assertNotEquals(GameTokenUtil.generateToken(), GameTokenUtil.generateToken());
    }

    @Test
    void sha256HexShouldBeStableAndLowercase() {
        String first = GameTokenUtil.sha256Hex("gs_test-token");
        String second = GameTokenUtil.sha256Hex("gs_test-token");

        assertEquals(first, second);
        assertTrue(first.matches("^[a-f0-9]{64}$"));
    }
}
