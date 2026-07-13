package com.thx.module.payment.scheduler;

import com.thx.module.payment.application.PaymentSyncService;
import com.thx.module.payment.config.PaymentProperties;
import com.thx.module.payment.domain.PaymentAttempt;
import com.thx.module.payment.repository.PaymentAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * 支付状态补偿定时任务：扫描结果未知 / 长时间停留在处理中或初始状态的 Attempt，主动查询渠道澄清。
 * 不需要 {@code SELECT ... FOR UPDATE SKIP LOCKED}：多实例即使扫到同一条记录也只是各自发起一次
 * 只读查询，最终都汇聚到 {@code PaymentChannelResultProcessor} 的行锁 + 状态机 + 事件唯一约束收敛，
 * 天然去重（见 docs/payment-architecture.md 并发场景清单）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconcileScheduler {

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentSyncService paymentSyncService;
    private final PaymentProperties paymentProperties;

    //@Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void reconcile() {
        PaymentProperties.Reconcile config = paymentProperties.getReconcile();
        if (!config.isEnabled()) {
            return;
        }
        Date staleBefore = Date.from(Instant.now().minus(config.getProcessingStaleMinutes(), ChronoUnit.MINUTES));
        List<PaymentAttempt> candidates = paymentAttemptRepository.findNeedsReconcile(staleBefore, config.getBatchSize());
        if (candidates.isEmpty()) {
            return;
        }
        log.info("支付状态补偿扫描到 {} 条待澄清记录", candidates.size());
        for (PaymentAttempt attempt : candidates) {
            try {
                paymentSyncService.syncAttempt(attempt);
            } catch (Exception e) {
                log.error("状态补偿处理单条记录失败: attemptNo={}", attempt.getAttemptNo(), e);
            }
        }
    }
}
