package com.thx.module.admin.vo.base;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Data
@AllArgsConstructor
public class ResponseVo<T> {

    private Integer status;
    private String msg;
    private T data;

    // 常用状态码常量
    public static final int SUCCESS = 200;
    public static final int ERROR = 500;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int UNAUTHORIZED = 401;

    public ResponseVo(Integer status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    // 成功响应的静态方法
    public static <T> ResponseVo<T> success() {
        return new ResponseVo<>(SUCCESS, "操作成功");
    }

    public static <T> ResponseVo<T> success(T data) {
        return new ResponseVo<>(SUCCESS, "操作成功", data);
    }

    public static <T> ResponseVo<T> success(String msg, T data) {
        return new ResponseVo<>(SUCCESS, msg, data);
    }

    // 失败响应的静态方法
    public static <T> ResponseVo<T> error() {
        return new ResponseVo<>(ERROR, "操作失败");
    }

    public static <T> ResponseVo<T> error(String msg) {
        return new ResponseVo<>(ERROR, msg);
    }

    public static <T> ResponseVo<T> error(int status, String msg) {
        return new ResponseVo<>(status, msg);
    }

    // 其他常用状态的静态方法
    public static <T> ResponseVo<T> forbidden(String msg) {
        return new ResponseVo<>(FORBIDDEN, msg);
    }

    public static <T> ResponseVo<T> notFound(String msg) {
        return new ResponseVo<>(NOT_FOUND, msg);
    }

    public static <T> ResponseVo<T> unauthorized(String msg) {
        return new ResponseVo<>(UNAUTHORIZED, msg);
    }
}
