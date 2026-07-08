package com.thx.common.util;


import com.thx.module.admin.entity.User;
import lombok.experimental.UtilityClass;
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource;

/**
 * 密码加密工具类，负责按 Shiro 约定的散列算法对用户密码做加盐哈希。
 * <p>
 * 这里的算法名（md5）和迭代次数（2）必须与 {@link com.thx.common.shiro.MyShiroRealm}
 * 中 {@code HashedCredentialsMatcher} 的配置保持一致——本类是"写入侧"（注册/改密时
 * 生成密文），MyShiroRealm 是"校验侧"（登录时比对密文），两处任一方单独改动都会导致
 * 已有用户无法登录，修改时务必同步调整。
 * <p>
 * 实际参与哈希运算的盐值并不是 {@link User#getSalt()} 本身，而是
 * {@link User#getCredentialsSalt()}（用户名 + 固定字符串 + 随机 salt 的组合），
 * 这样即使两个用户随机生成了相同的 salt，最终参与运算的盐也会因用户名不同而不同。
 */
@UtilityClass
public class PasswordHelper {
    /** Shiro 提供的安全随机数生成器，用于生成每个用户独立的随机 salt。 */
    private static final RandomNumberGenerator RANDOM_NUMBER_GENERATOR = new SecureRandomNumberGenerator();
    /** 散列算法名，需与 MyShiroRealm 中 HashedCredentialsMatcher 的配置保持一致。 */
    private static final String ALGORITHM_NAME = "md5";
    /** 散列迭代次数，需与 MyShiroRealm 中 HashedCredentialsMatcher 的配置保持一致。 */
    private static final int HASH_ITERATIONS = 2;

    /**
     * 为用户生成随机 salt 并对其明文密码（{@code user.getPassword()} 中当前存放的原始密码）
     * 做加盐哈希，哈希结果会直接覆盖回 {@code user.getPassword()}，随机 salt 写入
     * {@code user.getSalt()}。调用后应将 user 的 password 和 salt 一并持久化。
     *
     * @param user 待加密密码的用户，密码字段需先填入明文
     */
    public static void encryptPassword(User user) {
        user.setSalt(RANDOM_NUMBER_GENERATOR.nextBytes().toHex());
        String newPassword = new SimpleHash(ALGORITHM_NAME, user.getPassword(), ByteSource.Util.bytes(user.getCredentialsSalt()), HASH_ITERATIONS).toHex();
        user.setPassword(newPassword);
    }

    /**
     * 使用用户当前的 salt，对 {@code user.getPassword()} 中的明文密码做同样的加盐哈希运算，
     * 但不修改 user 对象，仅返回计算结果。常用于"输入的原密码是否正确"这类校验场景
     * （将计算结果与数据库中已存的密文比较）。
     *
     * @param user 用户，密码字段需为待校验的明文，salt 字段需为该用户已持久化的 salt
     * @return 加盐哈希后的密文
     */
    public static String getPassword(User user) {
        return new SimpleHash(ALGORITHM_NAME, user.getPassword(), ByteSource.Util.bytes(user.getCredentialsSalt()), HASH_ITERATIONS).toHex();
    }

    /**
     * 本地手工验证用的临时入口：生成一个用户名 admin、密码 123456 的加密结果并打印，
     * 便于开发时快速得到一条可直接写入数据库的密文+salt，非业务调用入口。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        User user = new User();
        user.setUsername("admin");
        user.setPassword("123456");
        encryptPassword(user);
        System.out.println(user);
    }
}
