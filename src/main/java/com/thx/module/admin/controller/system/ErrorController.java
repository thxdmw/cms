package com.thx.module.admin.controller.system;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 统一错误跳转页面（403/404/500），是 {@link com.thx.common.config.ErrorPageConfig} 和
 * {@link com.thx.exception.ExceptionHandleController} 里 forward:/error/xxx 最终落地的地方，
 * 渲染 templates/error/ 下对应的错误页模板。
 */
@Controller
@RequestMapping("/error")
@AllArgsConstructor
public class ErrorController {

    /** Shiro 鉴权失败（无权限访问）时进入 */
    @RequestMapping("/403")
    public String noPermission(HttpServletRequest request, HttpServletResponse response, Model model) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        return "error/403";
    }

    /** 资源不存在时进入 */
    @RequestMapping("/404")
    public String notFund(HttpServletRequest request, HttpServletResponse response, Model model) {
        return "error/404";
    }

    /** 服务器内部错误时进入 */
    @RequestMapping("/500")
    public String sysError(HttpServletRequest request, HttpServletResponse response, Model model) {
        return "error/500";
    }

}
