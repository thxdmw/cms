package com.thx.module.payment.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentClosedEvent {
    private String eventId;
    private String paymentNo;
    private String businessOrderNo;
    private Date closeTime;
}
