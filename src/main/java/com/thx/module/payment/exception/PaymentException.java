package com.thx.module.payment.exception;

import lombok.Getter;

/**
 * 支付模块统一业务异常基类。message 必须是可以直接返回给调用方的安全文案，
 * 不能包含渠道私钥、完整签名等敏感信息或底层异常堆栈细节。
 */
@Getter
public class PaymentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final PaymentErrorCode errorCode;

    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public PaymentException(PaymentErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PaymentException(PaymentErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
