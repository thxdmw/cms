package com.thx.module.payment.scheduler;

import com.thx.module.payment.application.PaymentEventDispatcher;
import com.thx.module.payment.config.PaymentProperties;
import com.thx.module.payment.domain.PaymentEvent;
import com.thx.module.payment.repository.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 支付事件补偿投递：处理未发布事件与到期重试的失败事件。当前是进程内事件分发，
 * 未来 Payment 拆成独立 Payment Center 后，本调度器可以替换/扩展为 Webhook Dispatcher，
 * 数据来源（payment_event 表）和投递判定逻辑不需要变。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventScheduler {

    private final PaymentEventRepository paymentEventRepository;
    private final PaymentEventDispatcher dispatcher;
    private final PaymentProperties paymentProperties;

    //@Scheduled(fixedDelay = 60 * 1000, initialDelay = 30 * 1000)
    public void redeliver() {
        PaymentProperties.Event config = paymentProperties.getEvent();
        if (!config.isEnabled()) {
            return;
        }
        List<PaymentEvent> due = paymentEventRepository.findDueForDelivery(new Date(), config.getBatchSize());
        if (due.isEmpty()) {
            return;
        }
        log.info("支付事件补偿扫描到 {} 条待投递事件", due.size());
        for (PaymentEvent event : due) {
            try {
                dispatcher.tryDispatch(event);
            } catch (Exception e) {
                log.error("支付事件补偿投递单条记录失败: eventId={}", event.getEventId(), e);
            }
        }
    }
}
