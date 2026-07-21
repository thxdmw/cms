package com.thx.module.gamesave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.thx.common.util.UUIDUtil;
import com.thx.module.gamesave.config.GameSaveProperties;
import com.thx.module.gamesave.dto.GameLoginRequest;
import com.thx.module.gamesave.dto.GameLoginResult;
import com.thx.module.gamesave.dto.GameRegisterRequest;
import com.thx.module.gamesave.dto.GameSessionResult;
import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameAccountMapper;
import com.thx.module.gamesave.mapper.GameDeviceMapper;
import com.thx.module.gamesave.model.GameAccount;
import com.thx.module.gamesave.model.GameDevice;
import com.thx.module.gamesave.service.GameAuthService;
import com.thx.module.gamesave.util.GamePasswordUtil;
import com.thx.module.gamesave.util.GameTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;
import java.util.regex.Pattern;

/** GameSave 注册、登录与设备 Token 轮换实现。 */
@Service
@RequiredArgsConstructor
public class GameAuthServiceImpl implements GameAuthService {

    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{8,64}$");

    private final GameAccountMapper gameAccountMapper;
    private final GameDeviceMapper gameDeviceMapper;
    private final GameSaveProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameLoginResult register(GameRegisterRequest request) {
        if (request == null) {
            throw GameSaveException.badRequest("INVALID_REGISTER_REQUEST", "注册请求不能为空");
        }
        validateIdentityFields(request.getUsername(), request.getPassword(), request.getDeviceId(), request.getDeviceName());
        if (request.getPassword().length() < 8 || request.getPassword().length() > 256) {
            throw GameSaveException.badRequest("INVALID_PASSWORD", "密码长度必须为 8-256 个字符");
        }

        GameAccount account = new GameAccount()
                .setUserId(UUIDUtil.uuid())
                .setUsername(request.getUsername().trim())
                .setPasswordHash(GamePasswordUtil.hashPassword(request.getPassword()))
                .setStatus(1);
        try {
            gameAccountMapper.insert(account);
        } catch (DuplicateKeyException duplicate) {
            throw GameSaveException.conflict("USERNAME_EXISTS", "用户名已存在");
        }
        return issueDeviceToken(account, request.getDeviceId(), request.getDeviceName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameLoginResult login(GameLoginRequest request) {
        if (request == null) {
            throw GameSaveException.badRequest("INVALID_LOGIN_REQUEST", "登录请求不能为空");
        }
        validateIdentityFields(request.getUsername(), request.getPassword(), request.getDeviceId(), request.getDeviceName());

        GameAccount account = gameAccountMapper.selectOne(new LambdaQueryWrapper<GameAccount>()
                .eq(GameAccount::getUsername, request.getUsername().trim())
                .eq(GameAccount::getStatus, 1)
                .last("LIMIT 1"));
        if (account == null || !GamePasswordUtil.matches(request.getPassword(), account.getPasswordHash())) {
            // 用户不存在和密码错误返回相同结果，避免账号探测。
            throw GameSaveException.unauthorized("INVALID_CREDENTIALS", "用户名或密码错误");
        }
        return issueDeviceToken(account, request.getDeviceId(), request.getDeviceName());
    }

    @Override
    public GameSessionResult getSession(GameCallerContext caller) {
        GameAccount account = gameAccountMapper.selectOne(new LambdaQueryWrapper<GameAccount>()
                .eq(GameAccount::getUserId, caller.getUserId())
                .eq(GameAccount::getStatus, 1)
                .last("LIMIT 1"));
        if (account == null) {
            throw GameSaveException.unauthorized("ACCOUNT_DISABLED", "账号不存在或已停用");
        }
        return new GameSessionResult(account.getUserId(), caller.getDeviceId(), account.getUsername());
    }

    /** 为当前账号创建或更新设备，并轮换设备 Token。 */
    private GameLoginResult issueDeviceToken(GameAccount account, String rawDeviceId, String rawDeviceName) {
        String deviceId = rawDeviceId.trim();
        GameDevice device = gameDeviceMapper.selectOne(new LambdaQueryWrapper<GameDevice>()
                .eq(GameDevice::getUserId, account.getUserId())
                .eq(GameDevice::getDeviceId, deviceId)
                .last("LIMIT 1"));
        if (device != null && !account.getUserId().equals(device.getUserId())) {
            throw GameSaveException.conflict("DEVICE_ID_CONFLICT", "设备标识已被其他账号占用");
        }

        String token = GameTokenUtil.generateToken();
        String tokenHash = GameTokenUtil.sha256Hex(token);
        Date now = new Date();
        Date tokenExpireTime = new Date(now.getTime()
                + Duration.ofDays(Math.max(1, properties.getTokenExpireDays())).toMillis());
        if (device == null) {
            device = new GameDevice()
                    .setDeviceId(deviceId)
                    .setUserId(account.getUserId())
                    .setDeviceName(rawDeviceName.trim())
                    .setTokenHash(tokenHash)
                    .setTokenExpireTime(tokenExpireTime)
                    .setLastSeenTime(now)
                    .setStatus(1);
            gameDeviceMapper.insert(device);
        } else {
            gameDeviceMapper.update(null, new LambdaUpdateWrapper<GameDevice>()
                    .eq(GameDevice::getId, device.getId())
                    .set(GameDevice::getDeviceName, rawDeviceName.trim())
                    .set(GameDevice::getTokenHash, tokenHash)
                    .set(GameDevice::getTokenExpireTime, tokenExpireTime)
                    .set(GameDevice::getLastSeenTime, now)
                    .set(GameDevice::getStatus, 1));
        }
        return new GameLoginResult(account.getUserId(), deviceId, token);
    }

    private void validateIdentityFields(String username,
                                        String password,
                                        String deviceId,
                                        String deviceName) {
        if (isBlank(username) || username.trim().length() > 64) {
            throw GameSaveException.badRequest("INVALID_USERNAME", "用户名不能为空且长度不能超过 64");
        }
        if (password == null || password.isEmpty() || password.length() > 256) {
            throw GameSaveException.badRequest("INVALID_PASSWORD", "密码不能为空且长度不能超过 256");
        }
        if (isBlank(deviceId) || !DEVICE_ID_PATTERN.matcher(deviceId.trim()).matches()) {
            throw GameSaveException.badRequest("INVALID_DEVICE_ID", "deviceId 仅允许 8-64 位字母、数字、下划线或短横线");
        }
        if (isBlank(deviceName) || deviceName.trim().length() > 128) {
            throw GameSaveException.badRequest("INVALID_DEVICE_NAME", "设备名称不能为空且长度不能超过 128");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
