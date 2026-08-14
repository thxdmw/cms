package com.thx.module.payment.channel.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelQueryRefundResult {
    private ChannelResultStatus resultStatus;
    private BigDecimal refundAmount;
    private String channelRefundNo;
    private String failureCode;
    private String failureMessage;
    private Map<String, Object> rawResponse;
}
