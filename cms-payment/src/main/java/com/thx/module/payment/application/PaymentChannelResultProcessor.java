package com.thx.module.payment.application;

import com.thx.module.payment.channel.spi.ChannelResultStatus;
import com.thx.module.payment.domain.PaymentAttempt;
import com.thx.module.payment.domain.PaymentAttemptStatus;
import com.thx.module.payment.domain.PaymentAuditAction;
import com.thx.module.payment.domain.PaymentOrder;
import com.thx.module.payment.domain.PaymentStateMachine;
import com.thx.module.payment.domain.PaymentStatus;
import com.thx.module.payment.exception.PaymentErrorCode;
import com.thx.module.payment.exception.PaymentException;
import com.thx.module.payment.exception.PaymentOrderNotFoundException;
import com.thx.module.payment.repository.PaymentAttemptRepository;
import com.thx.module.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 渠道结果（异步通知 / 主动查询）的统一处理入口。{@code AlipayNotifyService} 和
 * {@code PaymentSyncService}/{@code PaymentReconcileScheduler} 都必须调用本类，
 * 禁止各自实现一套"订单成功"逻辑。
 * <p>
 * 核心保证：{@code SELECT ... FOR UPDATE} 锁定目标订单行 + {@link PaymentStateMachine} 校验 +
 * 同事务写入 {@code PaymentEvent}（唯一约束兜底），使得 Notify 与 Query 并发发现同一次成功
 * 时只会产生一条 PAYMENT_SUCCEEDED 事件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentChannelResultProcessor {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentStateMachine stateMachine;
    private final PaymentEventService paymentEventService;
    private final PaymentAuditLogService auditLogService;

    @Transactional
    public PaymentOrder apply(PaymentAttempt attempt, ChannelResultStatus channelStatus, String channelTradeNo,
                               BigDecimal channelTotalAmount, String failureCode, String failureMessage) {
        String paymentNo = attempt.getPaymentNo();
        PaymentOrder order = paymentOrderRepository.lockByPaymentNo(paymentNo);
        if (order == null) {
            throw new PaymentOrderNotFoundException("支付订单不存在: paymentNo=" + paymentNo);
        }

        if (channelStatus == ChannelResultStatus.SUCCESS && channelTotalAmount != null
                && order.getAmount().compareTo(channelTotalAmount) != 0) {
            auditLogService.record(PaymentAuditAction.PAYMENT_ANOMALY_DETECTED, order.getAppCode(), paymentNo,
                    mapOf("reason", "AMOUNT_MISMATCH", "orderAmount", order.getAmount(), "channelAmount", channelTotalAmount));
            throw new PaymentException(PaymentErrorCode.PAYMENT_NOTIFY_AMOUNT_MISMATCH,
                    "渠道结果金额与本地订单不一致: paymentNo=" + paymentNo);
        }

        applyAttemptResult(attempt, channelStatus, channelTradeNo, failureCode, failureMessage);

        PaymentStatus currentStatus = PaymentStatus.valueOf(order.getStatus());
        PaymentStatus targetStatus = mapToOrderStatus(channelStatus);

        if (currentStatus == targetStatus) {
            log.debug("渠道结果与当前订单状态一致，幂等短路: paymentNo={}, status={}", paymentNo, currentStatus);
            return order;
        }

        if (!stateMachine.canTransition(currentStatus, targetStatus)) {
            if (stateMachine.isTerminalHighOrder(currentStatus)) {
                log.info("忽略迟到/不适用的渠道结果: paymentNo={}, current={}, target={}", paymentNo, currentStatus, targetStatus);
                return order;
            }
            auditLogService.record(PaymentAuditAction.PAYMENT_ANOMALY_DETECTED, order.getAppCode(), paymentNo,
                    mapOf("reason", "ILLEGAL_TRANSITION", "current", currentStatus, "target", targetStatus));
            log.warn("收到无法处理的渠道结果转换请求，已忽略: paymentNo={}, current={}, target={}",
                    paymentNo, currentStatus, targetStatus);
            return order;
        }

        Date now = new Date();
        Date successTime = targetStatus == PaymentStatus.SUCCESS ? now : order.getSuccessTime();
        Date closeTime = targetStatus == PaymentStatus.CLOSED ? now : order.getCloseTime();

        boolean updated = paymentOrderRepository.casUpdate(paymentNo, order.getVersion(), targetStatus,
                successTime, closeTime, order.getRefundedAmount());
        if (!updated) {
            // 本方法已经通过 FOR UPDATE 持有行锁，正常情况下不会发生版本冲突；出现即代表存在
            // 未经过行锁保护的写路径，属于需要立即排查的缺陷，不能静默吞掉。
            throw new PaymentException(PaymentErrorCode.PAYMENT_INTERNAL_ERROR,
                    "支付订单并发更新冲突: paymentNo=" + paymentNo);
        }
        order.setStatus(targetStatus.name());
        order.setSuccessTime(successTime);
        order.setCloseTime(closeTime);
        order.setVersion(order.getVersion() + 1);

        if (targetStatus == PaymentStatus.SUCCESS) {
            paymentEventService.createPaymentSucceededEvent(order, channelTradeNo);
            auditLogService.record(PaymentAuditAction.PAYMENT_SUCCEEDED, order.getAppCode(), paymentNo);
        } else if (targetStatus == PaymentStatus.CLOSED) {
            paymentEventService.createPaymentClosedEvent(order);
            auditLogService.record(PaymentAuditAction.PAYMENT_CLOSED, order.getAppCode(), paymentNo);
        } else if (targetStatus == PaymentStatus.FAILED) {
            auditLogService.record(PaymentAuditAction.PAYMENT_FAILED, order.getAppCode(), paymentNo);
        } else if (targetStatus == PaymentStatus.UNKNOWN) {
            auditLogService.record(PaymentAuditAction.PAYMENT_UNKNOWN, order.getAppCode(), paymentNo);
        }

        return order;
    }

    private void applyAttemptResult(PaymentAttempt attempt, ChannelResultStatus channelStatus, String channelTradeNo,
                                     String failureCode, String failureMessage) {
        PaymentAttemptStatus currentAttemptStatus = PaymentAttemptStatus.valueOf(attempt.getStatus());
        PaymentAttemptStatus targetAttemptStatus = mapToAttemptStatus(channelStatus);

        if (isAttemptTerminal(currentAttemptStatus) && channelTradeNo != null
                && attempt.getChannelTradeNo() != null && !attempt.getChannelTradeNo().equals(channelTradeNo)) {
            auditLogService.record(PaymentAuditAction.PAYMENT_ANOMALY_DETECTED, null, attempt.getPaymentNo(),
                    attempt.getAttemptNo(), null,
                    mapOf("reason", "ATTEMPT_CHANNEL_TRADE_NO_MISMATCH",
                            "stored", attempt.getChannelTradeNo(), "incoming", channelTradeNo), null);
            log.error("同一 Attempt 收到不同的渠道交易号，已忽略覆盖: attemptNo={}, stored={}, incoming={}",
                    attempt.getAttemptNo(), attempt.getChannelTradeNo(), channelTradeNo);
            return;
        }

        if (currentAttemptStatus == targetAttemptStatus) {
            return;
        }
        // Attempt 一旦到达终态不允许"降级"回非终态（如 SUCCESS 不会因为一次迟到的 UNKNOWN 查询结果被覆盖）
        if (isAttemptTerminal(currentAttemptStatus) && !isAttemptTerminal(targetAttemptStatus)) {
            return;
        }

        attempt.setStatus(targetAttemptStatus.name());
        if (channelTradeNo != null) {
            attempt.setChannelTradeNo(channelTradeNo);
        }
        if (failureCode != null) {
            attempt.setFailureCode(failureCode);
        }
        if (failureMessage != null) {
            attempt.setFailureMessage(failureMessage);
        }
        paymentAttemptRepository.update(attempt);
    }

    private boolean isAttemptTerminal(PaymentAttemptStatus status) {
        return status == PaymentAttemptStatus.SUCCESS || status == PaymentAttemptStatus.FAILED
                || status == PaymentAttemptStatus.CLOSED;
    }

    private PaymentAttemptStatus mapToAttemptStatus(ChannelResultStatus channelStatus) {
        switch (channelStatus) {
            case SUCCESS:
                return PaymentAttemptStatus.SUCCESS;
            case FAILED:
                return PaymentAttemptStatus.FAILED;
            case CLOSED:
                return PaymentAttemptStatus.CLOSED;
            case PROCESSING:
                return PaymentAttemptStatus.PROCESSING;
            default:
                return PaymentAttemptStatus.UNKNOWN;
        }
    }

    private PaymentStatus mapToOrderStatus(ChannelResultStatus channelStatus) {
        switch (channelStatus) {
            case SUCCESS:
                return PaymentStatus.SUCCESS;
            case FAILED:
                return PaymentStatus.FAILED;
            case CLOSED:
                return PaymentStatus.CLOSED;
            case PROCESSING:
                return PaymentStatus.PROCESSING;
            default:
                return PaymentStatus.UNKNOWN;
        }
    }

    private Map<String, Object> mapOf(Object... kvPairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kvPairs.length; i += 2) {
            map.put(String.valueOf(kvPairs[i]), kvPairs[i + 1]);
        }
        return map;
    }
}
