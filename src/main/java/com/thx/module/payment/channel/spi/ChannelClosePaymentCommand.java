package com.thx.module.payment.channel.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelClosePaymentCommand {
    private Long channelAccountId;
    private String outTradeNo;
}
