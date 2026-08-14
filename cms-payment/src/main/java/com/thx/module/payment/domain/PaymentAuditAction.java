package com.thx.module.payment.domain;

/**
 * {@code PaymentAuditLog} 的动作类型。
 */
public enum PaymentAuditAction {

    PAYMENT_CREATED,
    PAYMENT_ATTEMPT_CREATED,
    PAYMENT_PROCESSING,
    PAYMENT_UNKNOWN,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    PAYMENT_CLOSED,
    REFUND_CREATED,
    REFUND_SUCCEEDED,
    REFUND_FAILED,
    PAYMENT_EVENT_CREATED,
    PAYMENT_EVENT_PUBLISHED,
    PAYMENT_EVENT_FAILED,
    /** 收到与本地已有事实冲突的渠道结果（如不同 channelTradeNo 的重复成功声明），需要人工排查 */
    PAYMENT_ANOMALY_DETECTED
}
