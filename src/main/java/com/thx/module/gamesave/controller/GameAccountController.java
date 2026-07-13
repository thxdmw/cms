package com.thx.module.gamesave.controller;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.context.GameCallerContextResolver;
import com.thx.module.gamesave.dto.GameQuotaResult;
import com.thx.module.gamesave.service.GameQuotaService;
import com.thx.module.gamesave.vo.GameSaveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/** GameSave 当前账户信息接口。 */
@RestController
@RequestMapping("/api/game-save/v1/account")
@RequiredArgsConstructor
public class GameAccountController {

    private final GameQuotaService gameQuotaService;

    /** 返回当前账户物理内容对象的配额、已用容量和剩余容量。 */
    @GetMapping("/quota")
    public GameSaveResponse<GameQuotaResult> getQuota(HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        return GameSaveResponse.success(gameQuotaService.get(caller));
    }
}