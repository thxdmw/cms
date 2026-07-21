package com.thx.module.gamesave.controller;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.context.GameCallerContextResolver;
import com.thx.module.gamesave.dto.GameQuotaResult;
import com.thx.module.gamesave.dto.GameSessionResult;
import com.thx.module.gamesave.service.GameAuthService;
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
    private final GameAuthService gameAuthService;

    /** 返回当前 Token 绑定的账号和设备身份，用于客户端本地数据重置后恢复会话。 */
    @GetMapping("/session")
    public GameSaveResponse<GameSessionResult> getSession(HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        return GameSaveResponse.success(gameAuthService.getSession(caller));
    }

    /** 返回当前账户物理内容对象的配额、已用容量和剩余容量。 */
    @GetMapping("/quota")
    public GameSaveResponse<GameQuotaResult> getQuota(HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        return GameSaveResponse.success(gameQuotaService.get(caller));
    }
}
