package com.thx.module.gamesave.controller;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.context.GameCallerContextResolver;
import com.thx.module.gamesave.dto.GameRetentionCleanupResult;
import com.thx.module.gamesave.dto.GameRetentionPolicyRequest;
import com.thx.module.gamesave.dto.GameRetentionPolicyResult;
import com.thx.module.gamesave.service.GameRetentionService;
import com.thx.module.gamesave.vo.GameSaveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/** 单个云端游戏的快照保留策略接口。 */
@RestController
@RequestMapping("/api/game-save/v1/games/{gameId}/retention")
@RequiredArgsConstructor
public class GameRetentionController {

    private final GameRetentionService gameRetentionService;

    @GetMapping
    public GameSaveResponse<GameRetentionPolicyResult> get(@PathVariable String gameId,
                                                            HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        return GameSaveResponse.success(gameRetentionService.get(gameId, caller));
    }

    @PutMapping
    public GameSaveResponse<GameRetentionPolicyResult> update(
            @PathVariable String gameId,
            @RequestBody GameRetentionPolicyRequest request,
            HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        return GameSaveResponse.success("快照保留策略已更新", gameRetentionService.update(gameId, request, caller));
    }

    /** 用户可在保存策略后立即执行一次清理，不必等待后台调度。 */
    @PostMapping("/cleanup")
    public GameSaveResponse<GameRetentionCleanupResult> cleanup(
            @PathVariable String gameId,
            HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        return GameSaveResponse.success("快照保留策略已执行", gameRetentionService.cleanup(gameId, caller));
    }
}