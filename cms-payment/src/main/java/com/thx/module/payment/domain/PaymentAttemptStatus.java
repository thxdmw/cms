package com.thx.module.payment.domain;

/**
 * 单次渠道支付尝试状态。语义与 {@link PaymentStatus} 对齐，但不单独建状态机——
 * {@code PaymentAttempt} 记录的是"一次调用过程"，其正确性由
 * {@code PaymentChannelResultProcessor} 的业务编排保证。
 */
public enum PaymentAttemptStatus {

    /** 已创建，尚未调用渠道 */
    INIT,

    /** 已调用渠道，等待结果 */
    PROCESSING,

    /** 调用异常/超时，结果未知 */
    UNKNOWN,

    /** 渠道确认成功 */
    SUCCESS,

    /** 渠道明确失败 */
    FAILED,

    /** 已关闭 */
    CLOSED
}
