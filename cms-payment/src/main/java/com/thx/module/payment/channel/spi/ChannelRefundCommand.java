package com.thx.module.payment.channel.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelRefundCommand {
    private Long channelAccountId;
    private String outTradeNo;
    private BigDecimal refundAmount;
    /** 退款请求号，取值为 {@code RefundOrder.refundNo}，用于渠道侧的退款幂等 */
    private String outRequestNo;
    private String refundReason;
}
