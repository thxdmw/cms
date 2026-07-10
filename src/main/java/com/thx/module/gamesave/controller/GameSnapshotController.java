package com.thx.module.gamesave.controller;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.context.GameCallerContextResolver;
import com.thx.module.gamesave.dto.SnapshotCommitRequest;
import com.thx.module.gamesave.dto.SnapshotCommitResult;
import com.thx.module.gamesave.dto.SyncHeadResult;
import com.thx.module.gamesave.service.GameSnapshotService;
import com.thx.module.gamesave.vo.GameSaveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/** GameSave 云端 HEAD 与不可变快照提交接口。 */
@RestController
@RequestMapping("/api/game-save/v1/games/{gameId}")
@RequiredArgsConstructor
public class GameSnapshotController {

    private final GameSnapshotService gameSnapshotService;

    /** 获取指定游戏当前云端 HEAD。 */
    @GetMapping("/head")
    public GameSaveResponse<SyncHeadResult> getHead(@PathVariable String gameId,
                                                    HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        return GameSaveResponse.success(gameSnapshotService.getHead(gameId, caller));
    }

    /** 提交完整 Manifest；HEAD 已变化时返回 409 SYNC_CONFLICT。 */
    @PostMapping("/snapshots")
    @ResponseStatus(HttpStatus.CREATED)
    public GameSaveResponse<SnapshotCommitResult> commit(@PathVariable String gameId,
                                                          @RequestBody SnapshotCommitRequest request,
                                                          HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        return GameSaveResponse.success("快照提交成功", gameSnapshotService.commit(gameId, request, caller));
    }
}
