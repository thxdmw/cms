package com.thx.module.gamesave.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** GameSave API 统一响应体。 */
@Data
@AllArgsConstructor
public class GameSaveResponse<T> {

    private int status;
    private String code;
    private String msg;
    private T data;

    public static <T> GameSaveResponse<T> success(T data) {
        return new GameSaveResponse<>(200, "SUCCESS", "操作成功", data);
    }

    public static <T> GameSaveResponse<T> success(String message, T data) {
        return new GameSaveResponse<>(200, "SUCCESS", message, data);
    }

    public static <T> GameSaveResponse<T> error(int status, String code, String message) {
        return new GameSaveResponse<>(status, code, message, null);
    }
}
