package com.thx.module.payment.exception;

public class RefundAmountExceededException extends PaymentException {

    private static final long serialVersionUID = 1L;

    public RefundAmountExceededException(String message) {
        super(PaymentErrorCode.REFUND_AMOUNT_EXCEEDED, message);
    }
}
