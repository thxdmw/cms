package com.thx.module.payment.application;

import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.enums.PaymentScene;
import com.thx.module.payment.channel.spi.ChannelQueryPaymentCommand;
import com.thx.module.payment.channel.spi.ChannelQueryPaymentResult;
import com.thx.module.payment.channel.spi.ChannelResultStatus;
import com.thx.module.payment.channel.spi.PaymentChannelProvider;
import com.thx.module.payment.channel.spi.PaymentChannelRouter;
import com.thx.module.payment.domain.PaymentAttempt;
import com.thx.module.payment.domain.PaymentOrder;
import com.thx.module.payment.exception.PaymentOrderNotFoundException;
import com.thx.module.payment.repository.PaymentAttemptRepository;
import com.thx.module.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 本地查询与主动渠道查询。{@link #syncAttempt} 是与 {@code PaymentReconcileScheduler} 共用的核心方法，
 * 保证"业务主动 syncPayment"和"定时补偿扫描"最终都走 {@link PaymentChannelResultProcessor} 同一个收敛点。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSyncService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentChannelRouter channelRouter;
    private final PaymentChannelResultProcessor resultProcessor;

    public PaymentOrder queryPaymentLocal(String paymentNo) {
        PaymentOrder order = paymentOrderRepository.findByPaymentNo(paymentNo);
        if (order == null) {
            throw new PaymentOrderNotFoundException("支付订单不存在: paymentNo=" + paymentNo);
        }
        return order;
    }

    /**
     * 主动向渠道查询该订单最新一次尝试的结果并同步落库。
     */
    public PaymentOrder syncPayment(String paymentNo) {
        PaymentOrder order = queryPaymentLocal(paymentNo);
        PaymentAttempt attempt = paymentAttemptRepository.findLatestByPaymentNo(paymentNo);
        if (attempt == null) {
            log.warn("订单没有任何 Attempt，无法主动查询: paymentNo={}", paymentNo);
            return order;
        }
        return syncAttempt(attempt);
    }

    /**
     * 对单条 Attempt 主动发起渠道查询并应用结果，供 {@link #syncPayment} 与
     * {@code PaymentReconcileScheduler} 共用，是"渠道查询结果"这条路径唯一的入口。
     */
    public PaymentOrder syncAttempt(PaymentAttempt attempt) {
        PaymentChannelProvider provider = channelRouter.route(
                PaymentChannel.valueOf(attempt.getChannel()), PaymentScene.valueOf(attempt.getScene()));
        ChannelQueryPaymentCommand queryCommand = ChannelQueryPaymentCommand.builder()
                .channelAccountId(attempt.getChannelAccountId())
                .outTradeNo(attempt.getAttemptNo())
                .build();

        ChannelQueryPaymentResult result;
        try {
            result = provider.queryPayment(queryCommand);
        } catch (Exception e) {
            log.error("主动查询支付渠道发生未预期异常，判定为 UNKNOWN: attemptNo={}", attempt.getAttemptNo(), e);
            result = ChannelQueryPaymentResult.builder()
                    .resultStatus(ChannelResultStatus.UNKNOWN)
                    .failureCode("UNEXPECTED_ERROR")
                    .failureMessage(e.getMessage())
                    .build();
        }

        return resultProcessor.apply(attempt, result.getResultStatus(), result.getChannelTradeNo(),
                result.getTotalAmount(), result.getFailureCode(), result.getFailureMessage());
    }
}
