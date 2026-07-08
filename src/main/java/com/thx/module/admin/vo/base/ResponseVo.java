package com.thx.module.admin.vo.base;

import com.thx.enums.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 全局统一的接口响应体（file 模块有自己独立的同名类，不受此类影响，见该类注释）。
 * 状态码不在这里重复定义，统一取自 {@link ResponseStatus}。
 */
@Data
@AllArgsConstructor
public class ResponseVo<T> {

    private Integer status;
    private String msg;
    private T data;

    public ResponseVo(Integer status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    // 成功响应的静态方法
    public static <T> ResponseVo<T> success() {
        return new ResponseVo<>(ResponseStatus.SUCCESS.getCode(), "操作成功");
    }

    public static <T> ResponseVo<T> success(T data) {
        return new ResponseVo<>(ResponseStatus.SUCCESS.getCode(), "操作成功", data);
    }

    public static <T> ResponseVo<T> success(String msg, T data) {
        return new ResponseVo<>(ResponseStatus.SUCCESS.getCode(), msg, data);
    }

    // 失败响应的静态方法
    public static <T> ResponseVo<T> error() {
        return new ResponseVo<>(ResponseStatus.ERROR.getCode(), "操作失败");
    }

    public static <T> ResponseVo<T> error(String msg) {
        return new ResponseVo<>(ResponseStatus.ERROR.getCode(), msg);
    }

    public static <T> ResponseVo<T> error(int status, String msg) {
        return new ResponseVo<>(status, msg);
    }

    // 其他常用状态的静态方法
    public static <T> ResponseVo<T> forbidden(String msg) {
        return new ResponseVo<>(ResponseStatus.FORBIDDEN.getCode(), msg);
    }

    public static <T> ResponseVo<T> notFound(String msg) {
        return new ResponseVo<>(ResponseStatus.NOT_FOUND.getCode(), msg);
    }

    public static <T> ResponseVo<T> unauthorized(String msg) {
        return new ResponseVo<>(ResponseStatus.UNAUTHORIZED.getCode(), msg);
    }
}
