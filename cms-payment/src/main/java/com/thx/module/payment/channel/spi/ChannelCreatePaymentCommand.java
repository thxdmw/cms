package com.thx.module.payment.channel.spi;

import com.thx.module.payment.api.enums.PaymentScene;
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
public class ChannelCreatePaymentCommand {
    private Long channelAccountId;
    private PaymentScene scene;
    /** 提交给渠道的商户订单号，取值为 {@code PaymentAttempt.attemptNo} */
    private String outTradeNo;
    private String subject;
    private String description;
    private BigDecimal amount;
    private String currency;
    private String notifyUrl;
    private Date expireTime;
}
