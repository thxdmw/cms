package com.thx.module.payment.channel.spi;

import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.enums.PaymentScene;
import com.thx.module.payment.exception.UnsupportedPaymentChannelException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 按 (channel, scene) 挑选 {@link PaymentChannelProvider}。Application 层禁止出现
 * {@code if(channel==ALIPAY)}/{@code switch(channel)} 分支，一律通过本类解耦。
 */
@Component
@RequiredArgsConstructor
public class PaymentChannelRouter {

    private final List<PaymentChannelProvider> providers;

    public PaymentChannelProvider route(PaymentChannel channel, PaymentScene scene) {
        return providers.stream()
                .filter(p -> p.supports(channel, scene))
                .findFirst()
                .orElseThrow(() -> new UnsupportedPaymentChannelException(
                        "不支持的渠道/场景组合: channel=" + channel + ", scene=" + scene));
    }

    /**
     * 供异步通知场景使用：渠道已经由回调 URL Path 决定，不需要 scene 参与匹配。
     */
    public PaymentChannelProvider routeByChannel(PaymentChannel channel) {
        return providers.stream()
                .filter(p -> p.channel() == channel)
                .findFirst()
                .orElseThrow(() -> new UnsupportedPaymentChannelException("不支持的渠道: channel=" + channel));
    }
}
