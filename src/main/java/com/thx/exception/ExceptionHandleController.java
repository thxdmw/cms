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
import java.util.HashMap;
import java.util.Map;

/**
 * 异常统一处理
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Slf4j
@ControllerAdvice
public class ExceptionHandleController {

    @ExceptionHandler(AppException.class)
    public String handleAppException(Exception e, HttpServletRequest request) {
        request.setAttribute("javax.servlet.error.status_code", ResponseStatus.ERROR.getCode());
        Map<String, Object> map = new HashMap<>(2);
        map.put("status", ResponseStatus.ERROR.getCode());
        map.put("msg", StrUtil.isNotBlank(e.getMessage()) ? e.getMessage() : ResponseStatus.ERROR.getMessage());
        log.error("拦截到系统异常AppException: {}", e.getMessage(), e);
        request.setAttribute("ext", map);
        return "forward:/error";
    }

    @ExceptionHandler(ArticleNotFoundException.class)
    public String handleArticle(Exception e, HttpServletRequest request) {
        request.setAttribute("javax.servlet.error.status_code", ResponseStatus.NOT_FOUND.getCode());
        return "forward:/error";
    }

    @ExceptionHandler(AuthorizationException.class)
    public String handleAuth(HttpServletRequest request) {
        request.setAttribute("javax.servlet.error.status_code", ResponseStatus.FORBIDDEN.getCode());
        return "forward:/error";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, HttpServletRequest request) {
        log.error("URI: {} 捕获异常: {}", request.getRequestURI(), e.getMessage(), e);
        request.setAttribute("javax.servlet.error.status_code", ResponseStatus.ERROR.getCode());
        return "forward:/error";
    }

    @ExceptionHandler(UnknownSessionException.class)
    public String handleUnknownSessionException() {
        return "redirect:/login"; // 或返回 JSON，提示重新登录
    }

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
