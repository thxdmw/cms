package com.thx.module.payment.api.enums;

/**
 * 支付场景。当前只有 {@code ALIPAY+APP} 组合有真实实现。
 */
public enum PaymentScene {

    /** App 内调起（如支付宝 App SDK 支付） */
    APP,

    /** 手机浏览器 H5 支付（未实现） */
    H5,

    /** PC 网页支付（未实现） */
    WEB,

    /** 扫码支付（未实现） */
    QR
}
