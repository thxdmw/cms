package com.thx.module.file.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyUtilTest {

    @Test
    void sha256HexIsDeterministic() {
        assertEquals(ApiKeyUtil.sha256Hex("hello"), ApiKeyUtil.sha256Hex("hello"));
    }

    @Test
    void matchesReturnsTrueForCorrectKey() {
        String hash = ApiKeyUtil.sha256Hex("my-secret-key");
        assertTrue(ApiKeyUtil.matches("my-secret-key", hash));
    }

    @Test
    void matchesReturnsFalseForWrongKey() {
        String hash = ApiKeyUtil.sha256Hex("my-secret-key");
        assertFalse(ApiKeyUtil.matches("wrong-key", hash));
    }

    @Test
    void matchesReturnsFalseForNullInputs() {
        assertFalse(ApiKeyUtil.matches(null, "somehash"));
        assertFalse(ApiKeyUtil.matches("key", null));
    }
}
