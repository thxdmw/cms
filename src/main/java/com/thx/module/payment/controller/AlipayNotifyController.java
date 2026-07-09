package com.thx.module.payment.controller;

import com.thx.common.annotation.AnonymousAccess;
import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.application.PaymentNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝异步通知回调。调用方是支付宝服务器，不能走 Shiro 登录鉴权（{@link AnonymousAccess}），
 * 安全性完全依赖 {@code PaymentNotifyService} 内部的官方 SDK 验签，不做任何弱化。
 * <p>
 * 响应体必须是纯文本 "success"/"failure"（支付宝要求的固定协议），不能返回 JSON，
 * 因此本 Controller 不使用项目全局的 {@code ResponseVo}，且任何未预期异常都必须在本类内部兜底，
 * 不能让异常冒泡到 {@code PaymentExceptionHandler}（那会返回 JSON，支付宝无法识别为成功确认）。
 */
@Slf4j
@RestController
@RequestMapping("/api/payment/channel-notify")
@RequiredArgsConstructor
public class AlipayNotifyController {

    private static final String SUCCESS = "success";
    private static final String FAILURE = "failure";

    private final PaymentNotifyService notifyService;

    @AnonymousAccess
    @PostMapping("/alipay/{channelAccountCode}")
    public String notify(@PathVariable String channelAccountCode, HttpServletRequest request) {
        try {
            Map<String, String> params = extractParams(request);
            boolean success = notifyService.handleNotify(PaymentChannel.ALIPAY, channelAccountCode, params,
                    request.getRemoteAddr());
            return success ? SUCCESS : FAILURE;
        } catch (Exception e) {
            log.error("处理支付宝异步通知发生未预期异常: channelAccountCode={}", channelAccountCode, e);
            return FAILURE;
        }
    }

    /** 与支付宝官方示例一致的参数提取方式：多值参数用逗号拼接 */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            result.put(entry.getKey(), String.join(",", entry.getValue()));
        }
        return result;
    }
}
