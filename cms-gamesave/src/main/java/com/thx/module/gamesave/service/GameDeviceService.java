package com.thx.module.gamesave.service;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.dto.GameDeviceSummaryResult;

import java.util.List;

/** GameSave 已登录设备查询和撤销服务。 */
public interface GameDeviceService {

    List<GameDeviceSummaryResult> list(GameCallerContext caller);

    void revoke(String deviceId, GameCallerContext caller);
}