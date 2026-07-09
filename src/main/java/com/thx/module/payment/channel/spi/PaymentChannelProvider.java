package com.thx.module.payment.channel.spi;

import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.enums.PaymentScene;

/**
 * 支付渠道适配器 SPI。当前只有 {@code AlipayPaymentProvider} 是真实实现
 * （{@code channel()==ALIPAY, supports()} 仅对 {@code APP} 场景返回 true）。
 * 新增渠道时才新增实现类，不允许伪实现（方法体 {@code throw UnsupportedOperationException}）占位。
 */
public interface PaymentChannelProvider {

    PaymentChannel channel();

    boolean supports(PaymentChannel channel, PaymentScene scene);

    ChannelCreatePaymentResult createPayment(ChannelCreatePaymentCommand command);

    ChannelQueryPaymentResult queryPayment(ChannelQueryPaymentCommand command);

    ChannelClosePaymentResult closePayment(ChannelClosePaymentCommand command);

    ChannelRefundResult refund(ChannelRefundCommand command);

    ChannelQueryRefundResult queryRefund(ChannelQueryRefundCommand command);

    ChannelNotifyResult parseAndVerifyNotify(ChannelNotifyCommand command);
}
