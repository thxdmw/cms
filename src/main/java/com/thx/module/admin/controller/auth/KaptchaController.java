package com.thx.module.admin.controller.auth;

import io.springboot.captcha.ArithmeticCaptcha;
import io.springboot.captcha.utils.CaptchaUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录页图形验证码接口：生成算术类型验证码图片并直接写入响应流，配合 SystemController 的登录提交接口
 * 做人机校验（服务端校验逻辑见 CaptchaUtil.ver）。
 */
@Slf4j
@Controller
@AllArgsConstructor
public class KaptchaController {


    /**
     * 生成一道两位数算术验证码（图片尺寸 130x48）并写入响应，验证码答案保存在当前 session 中，供登录时校验。
     *
     * @param request  当前请求，用于关联 session
     * @param response 响应，验证码图片直接写入其输出流
     * @throws IOException 图片写出失败时抛出
     */
    @RequestMapping("/captcha")
    public void getCaptchaCode(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 48);
        captcha.setLen(2);
        CaptchaUtil.out(captcha, request, response);
    }
}
