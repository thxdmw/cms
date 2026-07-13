package com.thx.module.gamesave.dto;

import com.thx.module.gamesave.model.GameDevice;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

/** 面向设备管理界面的安全设备摘要，不返回 Token 或其哈希。 */
@Getter
@AllArgsConstructor
public class GameDeviceSummaryResult {

    private final String deviceId;
    private final String deviceName;
    private final Date lastSeenTime;
    private final boolean active;
    private final Date createTime;

    public static GameDeviceSummaryResult from(GameDevice device) {
        return new GameDeviceSummaryResult(
                device.getDeviceId(),
                device.getDeviceName(),
                device.getLastSeenTime(),
                Integer.valueOf(1).equals(device.getStatus()),
                device.getCreateTime());
    }
}