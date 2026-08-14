package com.thx.module.file.util;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * API Key 哈希与恒定时间比较工具
 * 禁止在任何地方明文保存或打印 API Key
 */
@UtilityClass
public class ApiKeyUtil {

    /** 计算 API Key 的 SHA-256 十六进制哈希 */
    public static String sha256Hex(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    /** 恒定时间比较两个哈希值，避免时序攻击 */
    public static boolean matches(String providedApiKey, String storedHash) {
        if (providedApiKey == null || storedHash == null) {
            return false;
        }
        String providedHash = sha256Hex(providedApiKey);
        return MessageDigest.isEqual(
                providedHash.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}
