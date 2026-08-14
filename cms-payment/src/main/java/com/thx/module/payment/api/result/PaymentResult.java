package com.thx.module.payment.api.result;

import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.enums.PaymentScene;
import com.thx.module.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {
    private String paymentNo;
    private String appCode;
    private String businessOrderNo;
    private PaymentStatus status;
    private PaymentChannel channel;
    private PaymentScene scene;
    private BigDecimal amount;
    private String currency;
    private BigDecimal refundedAmount;
    private Date successTime;
    private Date closeTime;
}
