package com.thx.module.payment.domain;

/**
 * 退款单状态。退款调用同样可能超时/结果未知，语义与 {@link PaymentAttemptStatus} 对齐。
 */
public enum RefundStatus {

    /** 已创建，尚未调用渠道 */
    INIT,

    /** 已调用渠道，处理中 */
    PROCESSING,

    /** 调用异常/超时，结果未知，需要主动查询 */
    UNKNOWN,

    /** 渠道确认退款成功 */
    SUCCESS,

    /** 渠道明确退款失败 */
    FAILED,

    /** 已关闭（不再重试） */
    CLOSED
}
