package com.thx.common.util;


import com.thx.module.admin.entity.User;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * <b>历史</b>密码散列算法（md5 + 2 次迭代 + 加盐）的兼容实现，<b>仅用于校验存量用户</b>。
 * <p>
 * <b>请勿在新代码中使用本类生成密码。</b>md5 是快速哈希，不适合存储密码。
 * 新密码一律通过 {@link com.thx.common.security.PasswordService#encode(String)} 用 BCrypt 生成；
 * 存量用户在下次登录成功时会被透明升级为 BCrypt，升级完成后本类将不再被触及。
 * <p>
 * 本类原先依赖 Shiro 的 {@code SimpleHash} 实现散列。为了让密码体系与鉴权框架解耦
 * （便于更换鉴权框架），这里改用 JDK 自带的 {@link MessageDigest} 重新实现了完全等价的算法：
 * Shiro 的 {@code SimpleHash(algorithm, source, salt, iterations)} 语义是
 * 先 {@code digest(salt_bytes + password_bytes)}，再把结果反复 digest（共 iterations 次），
 * 最后转小写十六进制。下面的实现与之逐字节一致，因此<b>历史密文可以照常校验通过</b>。
 * <p>
 * 实际参与运算的盐不是 {@link User#getSalt()} 本身，而是
 * {@link User#getCredentialsSalt()}（用户名 + 固定字符串 + 随机 salt 的组合）。
 */
@UtilityClass
public class PasswordHelper {

    /** 历史散列算法名。 */
    private static final String ALGORITHM_NAME = "MD5";
    /** 历史散列迭代次数。 */
    private static final int HASH_ITERATIONS = 2;
    /** 十六进制字符表，用于把摘要字节转成与 Shiro {@code toHex()} 一致的小写十六进制串。 */
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    /**
     * 用历史算法校验明文密码是否与用户存储的旧密文匹配。
     *
     * @param user        用户（需含 username、salt 与旧格式 password）
     * @param rawPassword 明文密码
     * @return 是否匹配
     */
    public static boolean matchesLegacy(User user, String rawPassword) {
        if (user == null || user.getPassword() == null || rawPassword == null) {
            return false;
        }
        String computed = legacyHash(rawPassword, user.getCredentialsSalt());
        // 使用恒定时间比较，避免通过响应时间差推断密文内容
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                user.getPassword().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 按历史算法计算散列值：md5(salt + password) 之后再迭代 md5 共 {@link #HASH_ITERATIONS} 次。
     *
     * @param rawPassword 明文密码
     * @param salt        组合盐（{@link User#getCredentialsSalt()}）
     * @return 小写十六进制散列串
     */
    private static String legacyHash(String rawPassword, String salt) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(ALGORITHM_NAME);
        } catch (NoSuchAlgorithmException e) {
            // MD5 是 JDK 强制要求实现的算法，正常环境不会走到这里
            throw new IllegalStateException("当前 JDK 不支持 " + ALGORITHM_NAME, e);
        }
        if (salt != null) {
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
        }
        byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
        // Shiro 的迭代语义：第 1 次已在上面完成，剩余 iterations-1 次对上一轮结果再做摘要
        for (int i = 1; i < HASH_ITERATIONS; i++) {
            digest.reset();
            hashed = digest.digest(hashed);
        }
        return toHex(hashed);
    }

    /**
     * 把字节数组转为小写十六进制字符串，与 Shiro {@code Hash.toHex()} 输出一致。
     *
     * @param bytes 摘要字节
     * @return 小写十六进制串
     */
    private static String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            chars[i * 2] = HEX_CHARS[v >>> 4];
            chars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(chars);
    }
}
