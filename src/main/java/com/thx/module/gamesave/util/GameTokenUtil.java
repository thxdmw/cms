package com.thx.module.gamesave.util;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * GameSave 设备 Token 工具。
 * 明文 Token 仅在签发时返回一次；服务端持久化层只保存 SHA-256。
 */
@UtilityClass
public class GameTokenUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 生成 256 bit 随机设备 Token，并增加产品前缀方便人工识别。 */
    public static String generateToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "gs_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /** 计算 Token 的 SHA-256 十六进制字符串。 */
    public static String sha256Hex(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}
