package com.thx.module.payment.api.event;

import com.thx.module.payment.api.enums.PaymentChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 支付成功事件负载。业务模块必须根据 {@link #eventId} 做消费幂等（见 docs/payment-integration-example.md），
 * 不能仅凭 Android 客户端"支付成功"回调就发放业务权益。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSucceededEvent {
    private String eventId;
    private String paymentNo;
    private String businessOrderNo;
    private BigDecimal amount;
    private String currency;
    private PaymentChannel channel;
    private String channelTradeNo;
    private Map<String, Object> metadata;
    private Date successTime;
}
