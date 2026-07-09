package com.thx.module.payment.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付模块业务错误码，携带默认 HTTP 状态码，由 {@link PaymentExceptionHandler} 统一转换为响应。
 */
@Getter
@AllArgsConstructor
public enum PaymentErrorCode {

    PAYMENT_APP_NOT_FOUND(404, "业务方不存在"),
    PAYMENT_APP_DISABLED(403, "业务方已禁用"),
    PAYMENT_PARAM_INVALID(400, "请求参数不合法"),
    PAYMENT_ORDER_NOT_FOUND(404, "支付订单不存在"),
    PAYMENT_ORDER_CONFLICT(409, "支付订单参数冲突"),
    PAYMENT_ORDER_CLOSED(409, "支付订单已关闭"),
    PAYMENT_ORDER_FAILED(409, "支付订单已失败"),
    PAYMENT_ILLEGAL_STATE(409, "非法的支付状态转换"),
    PAYMENT_CHANNEL_NOT_SUPPORTED(400, "不支持的支付渠道"),
    PAYMENT_SCENE_NOT_SUPPORTED(400, "不支持的支付场景"),
    PAYMENT_CURRENCY_NOT_SUPPORTED(400, "不支持的币种"),
    PAYMENT_BINDING_NOT_FOUND(404, "未配置可用的渠道绑定"),
    PAYMENT_CHANNEL_ACCOUNT_NOT_FOUND(404, "渠道账号不存在"),
    PAYMENT_CHANNEL_ACCOUNT_DISABLED(403, "渠道账号已禁用"),
    PAYMENT_CHANNEL_ERROR(502, "支付渠道返回错误"),
    PAYMENT_CHANNEL_TIMEOUT(504, "支付渠道调用超时"),
    PAYMENT_CHANNEL_UNKNOWN_RESULT(409, "支付结果暂不可确认，请稍后重试"),
    PAYMENT_NOTIFY_VERIFY_FAILED(400, "异步通知验签失败"),
    PAYMENT_NOTIFY_AMOUNT_MISMATCH(400, "异步通知金额不匹配"),
    PAYMENT_NOTIFY_APP_MISMATCH(400, "异步通知应用身份不匹配"),
    REFUND_ORDER_NOT_FOUND(404, "退款单不存在"),
    REFUND_ORDER_CONFLICT(409, "退款单参数冲突"),
    REFUND_AMOUNT_EXCEEDED(409, "退款金额超过可退余额"),
    PAYMENT_MASTER_KEY_INVALID(500, "支付主密钥配置无效"),
    PAYMENT_INTERNAL_ERROR(500, "支付模块内部错误");

    private final int httpStatus;
    private final String defaultMessage;
}
