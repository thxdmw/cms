package com.thx.module.payment.domain;

/**
 * 支付事件类型，业务模块通过 {@code @PaymentEventHandler(eventType=...)} 订阅。
 */
public enum PaymentEventType {

    /** 支付成功 */
    PAYMENT_SUCCEEDED,

    /** 支付关闭 */
    PAYMENT_CLOSED,

    /** 退款成功 */
    REFUND_SUCCEEDED
}
