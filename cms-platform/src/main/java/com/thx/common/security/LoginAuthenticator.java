package com.thx.common.security;

import com.thx.module.admin.entity.User;
import com.thx.module.admin.mapper.UserMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 登录凭证校验服务，是密码认证的唯一入口。
 * <p>
 * 原先这段逻辑实现为 Shiro 的 {@code CredentialsMatcher}，由 Shiro 在
 * {@code subject.login()} 内部回调。迁移到 Sa-Token 后不再有 Realm/Matcher 概念，
 * 登录流程改为显式调用：查用户 → 校验状态 → 调用本类校验密码 → 登记登录态。
 * 本类因此变成一个普通的 Spring 服务，<b>不依赖任何鉴权框架</b>。
 * <p>
 * 除校验外，本类还负责历史 md5 密码的<b>透明升级</b>：校验通过且用户仍是旧格式时，
 * 立刻用 BCrypt 重新编码并回写数据库，用户无感。
 * <p>
 * 这里直接依赖 {@link UserMapper} 而非 {@code UserService} 落库，是为了避免
 * 与用户服务之间形成循环依赖。
 */
@Slf4j
@Service
@AllArgsConstructor
public class LoginAuthenticator {

    private final PasswordService passwordService;

    private final UserMapper userMapper;

    /**
     * 校验用户密码，并在需要时把历史 md5 密码透明升级为 BCrypt。
     *
     * @param user        待校验用户（需已从数据库加载）
     * @param rawPassword 用户输入的明文密码
     * @return 密码是否正确
     */
    public boolean authenticate(User user, String rawPassword) {
        if (user == null || rawPassword == null) {
            return false;
        }
        if (!passwordService.matches(user, rawPassword)) {
            return false;
        }
        upgradeLegacyPassword(user, rawPassword);
        return true;
    }

    /**
     * 若用户密码仍是历史 md5 格式，则升级为 BCrypt 并只更新 password 字段。
     * <p>
     * 升级失败不影响本次登录：密码已校验通过，登录照常放行，仅记录告警，
     * 下次登录会再次尝试升级。
     */
    private void upgradeLegacyPassword(User user, String rawPassword) {
        if (!passwordService.upgradeIfLegacy(user, rawPassword)) {
            return;
        }
        try {
            // 只带主键和新密文，避免把用户对象上的其它字段（如登录 IP）一并写回数据库
            User patch = new User();
            patch.setUserId(user.getUserId());
            patch.setPassword(user.getPassword());
            userMapper.updateById(patch);
        } catch (RuntimeException e) {
            log.warn("密码透明升级落库失败，本次登录不受影响, userId={}", user.getUserId(), e);
        }
    }
}
