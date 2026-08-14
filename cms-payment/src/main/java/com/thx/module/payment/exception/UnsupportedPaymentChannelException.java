package com.thx.module.payment.exception;

/**
 * {@code PaymentChannelRouter} 找不到匹配 (channel, scene) 的 {@code PaymentChannelProvider} 时抛出。
 */
public class UnsupportedPaymentChannelException extends PaymentException {

    private static final long serialVersionUID = 1L;

    public UnsupportedPaymentChannelException(String message) {
        super(PaymentErrorCode.PAYMENT_CHANNEL_NOT_SUPPORTED, message);
    }
}
