package com.thx.module.gamesave.controller;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.context.GameCallerContextResolver;
import com.thx.module.gamesave.dto.GameCreateRequest;
import com.thx.module.gamesave.dto.GameLibraryResult;
import com.thx.module.gamesave.service.GameLibraryService;
import com.thx.module.gamesave.vo.GameSaveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/** 云端逻辑游戏库 REST 接口。 */
@RestController
@RequestMapping("/api/game-save/v1/games")
@RequiredArgsConstructor
public class GameLibraryController {

    private final GameLibraryService gameLibraryService;

    @GetMapping
    public GameSaveResponse<List<GameLibraryResult>> list(HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        return GameSaveResponse.success(gameLibraryService.list(caller));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameSaveResponse<GameLibraryResult> create(@RequestBody GameCreateRequest request,
                                                       HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        return GameSaveResponse.success("游戏已加入云端游戏库", gameLibraryService.create(request, caller));
    }

    @DeleteMapping("/{gameId}")
    public GameSaveResponse<Void> delete(@PathVariable String gameId,
                                         HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        gameLibraryService.delete(gameId, caller);
        return GameSaveResponse.success("游戏及其云端存档已删除", null);
    }
}