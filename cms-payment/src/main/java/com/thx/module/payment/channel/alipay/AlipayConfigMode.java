package com.thx.module.payment.channel.alipay;

/**
 * 支付宝验签配置模式。当前只完整实现 {@link #PUBLIC_KEY}；{@link #CERTIFICATE} 字段结构已预留，
 * 但 {@code AlipayClientFactory} 遇到该模式会明确拒绝，不做伪实现。
 */
public enum AlipayConfigMode {
    PUBLIC_KEY,
    CERTIFICATE
}
