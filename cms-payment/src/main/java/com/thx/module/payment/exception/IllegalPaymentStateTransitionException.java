package com.thx.module.payment.exception;

import lombok.Getter;

/**
 * 非法的状态转换请求，由 {@link com.thx.module.payment.domain.PaymentStateMachine} 抛出。
 */
@Getter
public class IllegalPaymentStateTransitionException extends PaymentException {

    private static final long serialVersionUID = 1L;

    private final String from;
    private final String to;

    public IllegalPaymentStateTransitionException(String from, String to) {
        super(PaymentErrorCode.PAYMENT_ILLEGAL_STATE, "非法的支付状态转换: " + from + " -> " + to);
        this.from = from;
        this.to = to;
    }
}
