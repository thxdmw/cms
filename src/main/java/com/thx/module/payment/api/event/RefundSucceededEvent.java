package com.thx.module.payment.api.event;

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
public class RefundSucceededEvent {
    private String eventId;
    private String refundNo;
    private String paymentNo;
    private String businessRefundNo;
    private BigDecimal amount;
    private String currency;
    private Date successTime;
}
