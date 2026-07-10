package com.thx.module.gamesave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.thx.module.gamesave.dto.GameLoginRequest;
import com.thx.module.gamesave.dto.GameLoginResult;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameAccountMapper;
import com.thx.module.gamesave.mapper.GameDeviceMapper;
import com.thx.module.gamesave.model.GameAccount;
import com.thx.module.gamesave.model.GameDevice;
import com.thx.module.gamesave.service.GameAuthService;
import com.thx.module.gamesave.util.GamePasswordUtil;
import com.thx.module.gamesave.util.GameTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.regex.Pattern;

/** GameSave 登录与设备 Token 轮换实现。 */
@Service
@RequiredArgsConstructor
public class GameAuthServiceImpl implements GameAuthService {

    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{8,64}$");

    private final GameAccountMapper gameAccountMapper;
    private final GameDeviceMapper gameDeviceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameLoginResult login(GameLoginRequest request) {
        validateRequest(request);

        GameAccount account = gameAccountMapper.selectOne(new LambdaQueryWrapper<GameAccount>()
                .eq(GameAccount::getUsername, request.getUsername().trim())
                .eq(GameAccount::getStatus, 1)
                .last("LIMIT 1"));
        if (account == null || !GamePasswordUtil.matches(request.getPassword(), account.getPasswordHash())) {
            // 用户不存在和密码错误返回相同结果，避免账号探测。
            throw GameSaveException.unauthorized("INVALID_CREDENTIALS", "用户名或密码错误");
        }

        String deviceId = request.getDeviceId().trim();
        GameDevice device = gameDeviceMapper.selectOne(new LambdaQueryWrapper<GameDevice>()
                .eq(GameDevice::getDeviceId, deviceId)
                .last("LIMIT 1"));
        if (device != null && !account.getUserId().equals(device.getUserId())) {
            throw GameSaveException.conflict("DEVICE_ID_CONFLICT", "设备标识已被其他账号占用");
        }

        String token = GameTokenUtil.generateToken();
        String tokenHash = GameTokenUtil.sha256Hex(token);
        Date now = new Date();
        if (device == null) {
            device = new GameDevice()
                    .setDeviceId(deviceId)
                    .setUserId(account.getUserId())
                    .setDeviceName(request.getDeviceName().trim())
                    .setTokenHash(tokenHash)
                    .setLastSeenTime(now)
                    .setStatus(1);
            gameDeviceMapper.insert(device);
        } else {
            gameDeviceMapper.update(null, new LambdaUpdateWrapper<GameDevice>()
                    .eq(GameDevice::getId, device.getId())
                    .set(GameDevice::getDeviceName, request.getDeviceName().trim())
                    .set(GameDevice::getTokenHash, tokenHash)
                    .set(GameDevice::getLastSeenTime, now)
                    .set(GameDevice::getStatus, 1));
        }

        return new GameLoginResult(account.getUserId(), deviceId, token);
    }

    private void validateRequest(GameLoginRequest request) {
        if (request == null) {
            throw GameSaveException.badRequest("INVALID_LOGIN_REQUEST", "登录请求不能为空");
        }
        if (isBlank(request.getUsername()) || request.getUsername().trim().length() > 64) {
            throw GameSaveException.badRequest("INVALID_USERNAME", "用户名不能为空且长度不能超过 64");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw GameSaveException.badRequest("INVALID_PASSWORD", "密码不能为空");
        }
        if (isBlank(request.getDeviceId()) || !DEVICE_ID_PATTERN.matcher(request.getDeviceId().trim()).matches()) {
            throw GameSaveException.badRequest("INVALID_DEVICE_ID", "deviceId 仅允许 8-64 位字母、数字、下划线或短横线");
        }
        if (isBlank(request.getDeviceName()) || request.getDeviceName().trim().length() > 128) {
            throw GameSaveException.badRequest("INVALID_DEVICE_NAME", "设备名称不能为空且长度不能超过 128");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
