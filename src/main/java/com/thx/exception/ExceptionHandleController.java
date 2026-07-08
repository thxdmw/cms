package com.thx.exception;

import cn.hutool.core.util.StrUtil;
import com.thx.enums.ResponseStatus;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.session.UnknownSessionException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 全局异常统一处理，覆盖走页面跳转的传统控制器和走 JSON 的 REST 接口两类场景：
 * 大多数异常被转发到 /error 页面（由 {@link com.thx.common.config.ErrorPageConfig} 之外的
 * 补充兜底逻辑，处理 Controller 方法内部抛出的、而非容器级别的异常）；
 * {@link ApiException} 是例外，会被 handleApi 转成 JSON 响应。
 */
@Slf4j
@ControllerAdvice
public class ExceptionHandleController {

    /** Shiro 鉴权失败（无权限访问），转发到 403 错误页 */
    @ExceptionHandler(AuthorizationException.class)
    public String handleAuth(HttpServletRequest request) {
        request.setAttribute("javax.servlet.error.status_code", ResponseStatus.FORBIDDEN.getCode());
        return "forward:/error";
    }

    /** 兜底处理所有未被其它 handler 覆盖的异常，记录错误日志后转发到 500 错误页 */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, HttpServletRequest request) {
        log.error("URI: {} 捕获异常: {}", request.getRequestURI(), e.getMessage(), e);
        request.setAttribute("javax.servlet.error.status_code", ResponseStatus.ERROR.getCode());
        return "forward:/error";
    }

    /** Shiro Session 已失效/不存在（如服务端重启导致内存 Session 丢失），引导用户重新登录 */
    @ExceptionHandler(UnknownSessionException.class)
    public String handleUnknownSessionException() {
        return "redirect:/login"; // 或返回 JSON，提示重新登录
    }

    /** {@link ApiException} 走 JSON 响应而不是页面跳转，供纯 REST 风格的接口（如 tools/agent 模块）使用 */
    @ExceptionHandler(ApiException.class)
    @ResponseBody
    public ResponseVo<Object> handleApi(Exception e, HttpServletRequest request, HttpServletResponse response) {
        log.error("URI: {} 捕获异常: {}", request.getRequestURI(), e.getMessage(), e);
        response.setStatus(ResponseStatus.ERROR.getCode());
        response.setContentType("application/json;charset=UTF-8");
        String message = StrUtil.isNotBlank(e.getMessage()) ? e.getMessage() : ResponseStatus.ERROR.getMessage();
        return new ResponseVo<>(ResponseStatus.ERROR.getCode(), message);
    }

}
