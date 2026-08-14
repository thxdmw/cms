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
public class ChannelCreatePaymentResult {
    private ChannelResultStatus resultStatus;
    /** 客户端拉起支付所需的数据，如 App 支付的 {@code {"orderStr": "..."}} */
    private Map<String, Object> payData;
    private String channelTradeNo;
    private String failureCode;
    private String failureMessage;
    /** 脱敏后的渠道原始响应快照，供审计落库 */
    private Map<String, Object> rawResponse;
}
