package com.thx.module.gamesave.controller;

import com.thx.module.gamesave.context.GameCallerContext;
import com.thx.module.gamesave.context.GameCallerContextResolver;
import com.thx.module.gamesave.dto.GameObjectResult;
import com.thx.module.gamesave.dto.ObjectCheckRequest;
import com.thx.module.gamesave.dto.ObjectDescriptor;
import com.thx.module.gamesave.model.GameObject;
import com.thx.module.gamesave.service.GameObjectService;
import com.thx.module.gamesave.vo.GameSaveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/** GameSave 内容对象检查与上传接口。 */
@RestController
@RequestMapping("/api/game-save/v1/objects")
@RequiredArgsConstructor
public class GameObjectController {

    private final GameObjectService gameObjectService;

    /** 返回当前用户尚未持有的 SHA-256 内容对象。 */
    @PostMapping("/check")
    public GameSaveResponse<List<ObjectDescriptor>> checkMissing(@RequestBody ObjectCheckRequest request,
                                                                  HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        return GameSaveResponse.success(gameObjectService.findMissing(request.getObjects(), caller));
    }

    /** 上传一个缺失对象；服务端会重新计算并校验真实 SHA-256 和大小。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameSaveResponse<GameObjectResult> upload(@RequestParam("file") MultipartFile file,
                                                      @RequestParam("sha256") String sha256,
                                                      @RequestParam("size") long size,
                                                      HttpServletRequest servletRequest) {
        GameCallerContext caller = GameCallerContextResolver.resolve(servletRequest);
        GameObject object = gameObjectService.put(file, sha256, size, caller);
        return GameSaveResponse.success("对象已就绪", GameObjectResult.from(object));
    }
}
