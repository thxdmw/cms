package com.thx.module.payment.exception;

import lombok.Getter;

/**
 * 渠道调用异常，携带渠道原始错误信息便于排查，但 {@link #getMessage()} 本身
 * 是脱敏后的安全文案（不包含堆栈、私钥、完整签名等）。
 */
@Getter
public class PaymentChannelException extends PaymentException {

    private static final long serialVersionUID = 1L;

    private final String channel;
    private final String channelErrorCode;
    private final String channelErrorMessage;

    public PaymentChannelException(PaymentErrorCode errorCode, String channel, String channelErrorCode, String channelErrorMessage) {
        super(errorCode, "支付渠道调用异常: channel=" + channel + ", errorCode=" + channelErrorCode);
        this.channel = channel;
        this.channelErrorCode = channelErrorCode;
        this.channelErrorMessage = channelErrorMessage;
    }
}
