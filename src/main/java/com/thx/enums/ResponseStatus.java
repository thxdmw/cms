package com.thx.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局统一的接口响应状态码枚举。
 * 这是全项目（file 模块除外，它有自己独立的一套响应体和状态码，属于有意为之的模块边界）
 * 状态码的唯一来源，{@link com.thx.module.admin.vo.base.ResponseVo} 和
 * {@link com.thx.common.util.ResultUtil} 都以这里的枚举值为准，不再各自重复定义。
 */
@Getter
@AllArgsConstructor
public enum ResponseStatus {

    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功！"),
    /**
     * 未登录或登录已失效
     */
    UNAUTHORIZED(401, "未登录或登录已失效！"),
    /**
     * 无权限
     */
    FORBIDDEN(403, "您没有权限访问！"),
    /**
     * 未找到
     */
    NOT_FOUND(404, "资源不存在！"),
    /**
     * 服务器内部错误
     */
    ERROR(500, "服务器内部错误！");

    private final Integer code;
    private final String message;

}
