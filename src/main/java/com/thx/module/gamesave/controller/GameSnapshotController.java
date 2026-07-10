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
    /** 返回当前用户可见的快照时间线；限制在 1 到 200 条，避免无界查询。 */
    @GetMapping("/snapshots")
    public GameSaveResponse<java.util.List<com.thx.module.gamesave.dto.SnapshotSummaryResult>> listSnapshots(
            @PathVariable String gameId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "100") int limit,
            HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        return GameSaveResponse.success(gameSnapshotService.listSnapshots(gameId, limit, caller));
    }
    /** 读取指定快照的完整文件清单，用于客户端安全恢复前的下载和校验。 */
    @GetMapping("/snapshots/{snapshotId}")
    public GameSaveResponse<com.thx.module.gamesave.dto.SnapshotManifestResult> getSnapshot(
            @PathVariable String gameId,
            @PathVariable String snapshotId,
            HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        return GameSaveResponse.success(gameSnapshotService.getSnapshot(gameId, snapshotId, caller));
    }

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
