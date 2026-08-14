package com.thx.module.payment.api.result;

import com.thx.module.payment.domain.RefundStatus;
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
public class RefundResult {
    private String refundNo;
    private String paymentNo;
    private RefundStatus status;
    private BigDecimal amount;
    private String failureCode;
    private String failureMessage;
    private Date successTime;
}
