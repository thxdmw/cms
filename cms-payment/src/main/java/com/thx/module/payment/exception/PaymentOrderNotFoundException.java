package com.thx.module.payment.exception;

public class PaymentOrderNotFoundException extends PaymentException {

    private static final long serialVersionUID = 1L;

    public PaymentOrderNotFoundException(String message) {
        super(PaymentErrorCode.PAYMENT_ORDER_NOT_FOUND, message);
    }
}
