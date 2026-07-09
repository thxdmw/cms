package com.thx.module.payment.exception;

/**
 * 同一 (appCode, businessOrderNo) 重复创建支付，但金额/币种等资金安全相关字段不一致。
 */
public class PaymentOrderConflictException extends PaymentException {

    private static final long serialVersionUID = 1L;

    public PaymentOrderConflictException(String message) {
        super(PaymentErrorCode.PAYMENT_ORDER_CONFLICT, message);
    }
}
