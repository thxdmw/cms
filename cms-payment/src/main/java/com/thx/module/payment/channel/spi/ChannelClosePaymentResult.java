package com.thx.module.payment.channel.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelClosePaymentResult {
    private ChannelResultStatus resultStatus;
    private String failureCode;
    private String failureMessage;
    private Map<String, Object> rawResponse;
}
