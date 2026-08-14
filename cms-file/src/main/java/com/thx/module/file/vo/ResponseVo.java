package com.thx.module.file.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文件模块专用的统一响应包装类
 * 只在 com.thx.module.file 包下使用，与 CMS 其余模块的 ResponseVo 相互独立
 */
@Data
@AllArgsConstructor
public class ResponseVo<T> {

    /** HTTP 状态码或业务状态码 */
    private Integer status;
    /** 提示信息 */
    private String msg;
    /** 响应数据，失败时通常为 null */
    private T data;

    // 常用状态码常量
    /** 成功 */
    public static final int SUCCESS = 200;
    /** 服务器内部错误 */
    public static final int ERROR = 500;
    /** 无权限 */
    public static final int FORBIDDEN = 403;
    /** 资源不存在 */
    public static final int NOT_FOUND = 404;
    /** 未认证 */
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
