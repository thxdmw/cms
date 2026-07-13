package com.thx.module.gamesave.controller;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.context.GameCallerContextResolver;
import com.thx.module.gamesave.dto.GameDeviceSummaryResult;
import com.thx.module.gamesave.service.GameDeviceService;
import com.thx.module.gamesave.vo.GameSaveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/** 已认证用户的设备管理接口。 */
@RestController
@RequestMapping("/api/game-save/v1/devices")
@RequiredArgsConstructor
public class GameDeviceController {

    private final GameDeviceService gameDeviceService;

    @GetMapping
    public GameSaveResponse<List<GameDeviceSummaryResult>> list(HttpServletRequest request) {
        GameCallerContext caller = GameCallerContextResolver.resolve(request);
        return GameSaveResponse.success(gameDeviceService.list(caller));
    }

    @DeleteMapping("/{deviceId}")
    public GameSaveResponse<Void> revoke(@PathVariable String deviceId, HttpServletRequest request) {
        GameCallerContext caller = GameCallerContextResolver.resolve(request);
        gameDeviceService.revoke(deviceId, caller);
        return GameSaveResponse.success("设备已撤销", null);
    }
}