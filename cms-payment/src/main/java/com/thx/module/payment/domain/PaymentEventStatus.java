package com.thx.module.payment.domain;

/**
 * 支付事件（Outbox）投递状态。
 */
public enum PaymentEventStatus {

    /** 已写入，等待投递 */
    PENDING,

    /** 正在投递（CAS 占用状态，防止并发重复投递） */
    PUBLISHING,

    /** 已投递成功（所有匹配的 Handler 均处理成功） */
    PUBLISHED,

    /** 投递失败，等待按退避策略重试 */
    FAILED
}
