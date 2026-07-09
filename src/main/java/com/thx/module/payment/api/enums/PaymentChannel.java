package com.thx.module.payment.api.enums;

/**
 * 支付渠道。当前只有 {@link #ALIPAY} 有真实的 {@code PaymentChannelProvider} 实现，
 * 其余枚举值仅作为架构预留，调用会在 {@code PaymentChannelRouter} 层收到明确的不支持异常。
 */
public enum PaymentChannel {

    /** 支付宝 */
    ALIPAY,

    /** 微信支付（未实现） */
    WECHAT_PAY,

    /** Stripe（未实现） */
    STRIPE,

    /** PayPal（未实现） */
    PAYPAL
}
