package com.thx.module.gamesave.exception;

import lombok.Getter;

/** GameSave 模块业务异常。 */
@Getter
public class GameSaveException extends RuntimeException {

    private final int status;
    private final String code;

    public GameSaveException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static GameSaveException badRequest(String code, String message) {
        return new GameSaveException(400, code, message);
    }
}
