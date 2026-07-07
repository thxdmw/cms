package com.thx.module.file.util;

import lombok.experimental.UtilityClass;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA256 计算工具
 * 上传时应该用 sha256Digest() 包一层 DigestInputStream 直接传给存储客户端，
 * 做到一次读取同时完成哈希计算和上传，不要读两遍文件。
 */
@UtilityClass
public class FileHashUtil {

    /** 创建一个 SHA-256 摘要器，配合 DigestInputStream 包裹原始流使用 */
    public static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    /** 将摘要字节数组转换为小写十六进制字符串 */
    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
