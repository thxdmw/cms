package com.thx.module.payment.domain;

/**
 * 支付订单状态。合法转换规则见 {@link PaymentStateMachine}。
 * <p>
 * 核心语义约束：{@link #FAILED} 只表示"渠道明确的业务失败"，绝不能表示
 * "本地调用失败/网络超时/结果未知"——那些一律落 {@link #UNKNOWN}。
 */
public enum PaymentStatus {

    /** 订单已创建，尚未成功发起渠道支付请求 */
    CREATED,

    /** 已成功发起渠道支付请求，等待用户完成支付 */
    PROCESSING,

    /** 渠道调用异常/超时，本地无法确认结果，需要主动查询澄清 */
    UNKNOWN,

    /** 支付成功（渠道明确返回成功） */
    SUCCESS,

    /** 渠道明确返回业务失败（不是网络/超时问题） */
    FAILED,

    /** 已关闭（未支付超时关闭或主动关闭） */
    CLOSED,

    /** 已部分退款 */
    PARTIALLY_REFUNDED,

    /** 已全额退款 */
    REFUNDED
}
