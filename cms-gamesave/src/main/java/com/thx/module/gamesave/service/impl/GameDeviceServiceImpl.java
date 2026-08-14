package com.thx.module.gamesave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.GameDeviceSummaryResult;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.mapper.GameDeviceMapper;
import com.thx.module.gamesave.model.GameDevice;
import com.thx.module.gamesave.service.GameDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 使用逻辑禁用撤销设备 Token；旧 Token 会立刻无法通过认证拦截器。 */
@Service
@RequiredArgsConstructor
public class GameDeviceServiceImpl implements GameDeviceService {

    private final GameDeviceMapper gameDeviceMapper;

    @Override
    public List<GameDeviceSummaryResult> list(GameCallerContext caller) {
        List<GameDevice> devices = gameDeviceMapper.selectList(new LambdaQueryWrapper<GameDevice>()
                .eq(GameDevice::getUserId, caller.getUserId())
                .orderByDesc(GameDevice::getLastSeenTime));
        List<GameDeviceSummaryResult> results = new ArrayList<>(devices.size());
        for (GameDevice device : devices) {
            results.add(GameDeviceSummaryResult.from(device));
        }
        return results;
    }

    @Override
    public void revoke(String deviceId, GameCallerContext caller) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw GameSaveException.badRequest("INVALID_DEVICE_ID", "设备 ID 不能为空");
        }
        String normalizedDeviceId = deviceId.trim();
        if (normalizedDeviceId.equals(caller.getDeviceId())) {
            throw GameSaveException.badRequest("CANNOT_REVOKE_CURRENT_DEVICE", "不能撤销当前正在使用的设备");
        }
        int updated = gameDeviceMapper.revokeActiveDevice(normalizedDeviceId, caller.getUserId());
        if (updated != 1) {
            throw GameSaveException.notFound("DEVICE_NOT_FOUND", "设备不存在或已经撤销");
        }
    }
}