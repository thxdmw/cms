package com.thx.module.gamesave.exception;

import com.thx.module.gamesave.vo.GameSaveResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** GameSave REST 接口异常处理器，禁止把内部堆栈直接返回给桌面客户端。 */
@Slf4j
@RestControllerAdvice(basePackages = "com.thx.module.gamesave")
public class GameSaveExceptionHandler {

    @ExceptionHandler(GameSaveException.class)
    public ResponseEntity<GameSaveResponse<Void>> handleGameSaveException(GameSaveException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(GameSaveResponse.<Void>error(
                        exception.getStatus(), exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GameSaveResponse<Void>> handleUnknownException(Exception exception) {
        log.error("GameSave 接口发生未处理异常", exception);
        return ResponseEntity.status(500)
                .body(GameSaveResponse.<Void>error(500, "INTERNAL_ERROR", "服务器内部错误"));
    }
}
