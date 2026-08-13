package com.thx.exception;

import cn.hutool.core.util.StrUtil;
import com.thx.enums.ResponseStatus;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.thx.common.util.JsonUtil;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常统一处理，覆盖走页面跳转的传统控制器和走 JSON 的 REST 接口两类场景：
 * 大多数异常被转发到 /error 页面（由 {@link com.thx.common.config.ErrorPageConfig} 之外的
 * 补充兜底逻辑，处理 Controller 方法内部抛出的、而非容器级别的异常）；
 * {@link ApiException} 是例外，会被 handleApi 转成 JSON 响应。
 */
@Slf4j
@ControllerAdvice
public class ExceptionHandleController {

    /** 鉴权失败（已登录但无权限/无角色），转发到 403 错误页 */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public String handleAuth(HttpServletRequest request) {
        request.setAttribute("jakarta.servlet.error.status_code", ResponseStatus.FORBIDDEN.getCode());
        return "forward:/error";
    }

    /** 兜底处理所有未被其它 handler 覆盖的异常，记录错误日志后转发到 500 错误页 */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, HttpServletRequest request) {
        log.error("URI: {} 捕获异常: {}", request.getRequestURI(), e.getMessage(), e);
        request.setAttribute("jakarta.servlet.error.status_code", ResponseStatus.ERROR.getCode());
        return "forward:/error";
    }

    /**
     * 未登录 / 登录态失效，统一在这里引导用户重新登录。
     * <p>
     * 该处理器承接了原先分散在两处的逻辑：Shiro 的 {@code AnnoOrLoginFilter} 负责未登录跳转，
     * {@code KickoutSessionControlFilter} 负责被顶下线的提示。Sa-Token 把这些情况统一抛为
     * {@link NotLoginException}，通过 {@code getType()} 区分具体原因：
     * <ul>
     *   <li>{@code BE_REPLACED}：账号在别处登录，当前会话被挤下线（超出 max-login-count）；</li>
     *   <li>{@code KICK_OUT}：被管理员从"在线用户"里强制踢下线；</li>
     *   <li>其余：未携带 token、token 无效或已超时。</li>
     * </ul>
     * 前两种对应原来的 /kickout 提示页，其余跳转登录页。Ajax 请求不做重定向
     * （否则前端拿到的是登录页 HTML），而是返回与原实现一致的 {@code user_status=300} JSON。
     */
    @ExceptionHandler(NotLoginException.class)
    public String handleNotLogin(NotLoginException e, HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {
        boolean kickedOut = NotLoginException.BE_REPLACED.equals(e.getType())
                || NotLoginException.KICK_OUT.equals(e.getType());

        if (isAjaxRequest(request)) {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("user_status", "300");
            body.put("message", kickedOut ? "您已经在其他地方登录，请重新登录！" : "登录状态已失效，请重新登录！");
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JsonUtil.toJson(body));
            response.getWriter().flush();
            return null;
        }
        return kickedOut ? "redirect:/kickout" : "redirect:/login";
    }

    /**
     * 判断是否为 Ajax 请求，判定方式与原 KickoutSessionControlFilter 保持一致。
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
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
