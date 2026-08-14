package com.thx.common.security;

import com.thx.common.util.PasswordHelper;
import com.thx.module.admin.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 密码编码与校验服务，是全项目密码相关逻辑的唯一入口。
 * <p>
 * <b>为什么要有这个类</b>：项目历史上用 md5 + 2 次迭代 + 加盐（见 {@link PasswordHelper}）存储密码，
 * md5 属于快速哈希，面对 GPU 暴力破解和彩虹表已不具备安全性。本类把新密码统一改用
 * <b>BCrypt</b>（自适应慢哈希，自带随机盐），并对存量用户提供<b>透明升级</b>：
 * 老用户下次登录时，只要用旧算法校验通过，就立刻用 BCrypt 重新编码并回写数据库，
 * 用户全程无感，也不需要强制所有人改密码。
 * <p>
 * <b>刻意与具体安全框架解耦</b>：本类不依赖 Shiro / Sa-Token 等任何鉴权框架，
 * 只依赖 {@link User} 实体。鉴权框架侧只做一层薄适配（调用 {@link #matches} 和
 * {@link #upgradeIfLegacy}），这样将来更换鉴权框架时，密码体系无需重写。
 */
@Slf4j
@Service
public class PasswordService {

    /**
     * BCrypt 生成的哈希固定以 {@code $2a$} / {@code $2b$} / {@code $2y$} 开头，
     * 用这个前缀区分"新格式"和"历史 md5 格式"，无需给数据库加算法标记字段。
     */
    private static final String BCRYPT_PREFIX = "$2";

    /** 强度 10 是 Spring Security 默认值，兼顾安全性与登录耗时（约几十毫秒）。 */
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(10);

    /**
     * 把明文密码编码为 BCrypt 哈希。BCrypt 自带随机盐，相同明文每次编码结果都不同，
     * 因此调用方<b>不需要</b>再自行生成或保存 salt。
     *
     * @param rawPassword 明文密码
     * @return BCrypt 哈希串（60 字符）
     */
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 判断一个已存储的密文是否是历史 md5 格式（即需要升级）。
     *
     * @param encodedPassword 数据库中存储的密文
     * @return true 表示是旧格式
     */
    public boolean isLegacyHash(String encodedPassword) {
        return encodedPassword != null && !encodedPassword.startsWith(BCRYPT_PREFIX);
    }

    /**
     * 校验明文密码是否与该用户存储的密码匹配，自动兼容新旧两种格式。
     * <p>
     * 本方法只做校验、不写库；升级动作请在校验通过后显式调用 {@link #upgradeIfLegacy}，
     * 避免"校验"这种只读语义的方法产生副作用。
     *
     * @param user        用户（需已从数据库加载，含 password 与 salt）
     * @param rawPassword 用户输入的明文密码
     * @return 是否匹配
     */
    public boolean matches(User user, String rawPassword) {
        if (user == null || user.getPassword() == null || rawPassword == null) {
            return false;
        }
        String stored = user.getPassword();
        if (isLegacyHash(stored)) {
            return PasswordHelper.matchesLegacy(user, rawPassword);
        }
        return encoder.matches(rawPassword, stored);
    }

    /**
     * 若该用户仍是旧格式密码，则用 BCrypt 重新编码并把新密文写回 {@code user} 对象。
     * <p>
     * 本方法<b>只修改传入的 user 对象</b>，持久化由调用方负责——这样调用方可以自行决定
     * 用哪个 Service/Mapper 落库，避免本类反向依赖 UserService 造成循环依赖。
     * 调用前必须已经用 {@link #matches} 校验过明文密码，否则会把错误密码写进数据库。
     *
     * @param user        用户对象
     * @param rawPassword 已校验通过的明文密码
     * @return true 表示确实执行了升级（调用方需要落库），false 表示无需升级
     */
    public boolean upgradeIfLegacy(User user, String rawPassword) {
        if (user == null || !isLegacyHash(user.getPassword())) {
            return false;
        }
        user.setPassword(encode(rawPassword));
        log.info("用户密码已从 md5 透明升级为 BCrypt, userId={}", user.getUserId());
        return true;
    }
}
