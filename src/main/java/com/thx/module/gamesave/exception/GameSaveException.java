package com.thx.module.gamesave.exception;

import lombok.Getter;

/** GameSave 模块业务异常，统一携带 HTTP 状态码和稳定业务错误码。 */
@Getter
public class GameSaveException extends RuntimeException {

    private final int status;
    private final String code;
    private final Integer retryAfterSeconds;

    public GameSaveException(int status, String code, String message) {
        this(status, code, message, null);
    }

    public GameSaveException(int status, String code, String message, Integer retryAfterSeconds) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static GameSaveException badRequest(String code, String message) {
        return new GameSaveException(400, code, message);
    }

    public static GameSaveException unauthorized(String code, String message) {
        return new GameSaveException(401, code, message);
    }

    public static GameSaveException forbidden(String code, String message) {
        return new GameSaveException(403, code, message);
    }

    public static GameSaveException notFound(String code, String message) {
        return new GameSaveException(404, code, message);
    }

    public static GameSaveException conflict(String code, String message) {
        return new GameSaveException(409, code, message);
    }

    public static GameSaveException tooManyRequests(String code, String message, int retryAfterSeconds) {
        return new GameSaveException(429, code, message, Math.max(1, retryAfterSeconds));
    }
}
