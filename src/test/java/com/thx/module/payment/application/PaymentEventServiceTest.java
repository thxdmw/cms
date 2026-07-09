package com.thx.module.payment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thx.module.payment.domain.PaymentEvent;
import com.thx.module.payment.domain.PaymentEventStatus;
import com.thx.module.payment.domain.PaymentOrder;
import com.thx.module.payment.infrastructure.PaymentNoGenerator;
import com.thx.module.payment.repository.PaymentEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付事件 Outbox 生命周期测试：SUCCESS 创建 Event 并触发实时投递信号，
 * 重复创建（唯一约束冲突）不重复触发信号，投递失败按退避表安排重试。
 */
@ExtendWith(MockitoExtension.class)
class PaymentEventServiceTest {

    @Mock
    private PaymentEventRepository paymentEventRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private PaymentEventService service;

    @BeforeEach
    void setUp() {
        service = new PaymentEventService(paymentEventRepository, new PaymentNoGenerator(),
                new ObjectMapper(), applicationEventPublisher);
    }

    private PaymentOrder successOrder() {
        return new PaymentOrder().setPaymentNo("P1").setAppCode("PET_APP").setBusinessOrderNo("B1")
                .setAmount(new BigDecimal("6.00")).setCurrency("CNY").setChannel("ALIPAY")
                .setStatus("SUCCESS").setSuccessTime(new Date());
    }

    @Test
    void createPaymentSucceededEventPublishesSignalWhenInserted() {
        when(paymentEventRepository.insertIfAbsent(any())).thenReturn(true);

        service.createPaymentSucceededEvent(successOrder(), "T1");

        ArgumentCaptor<PaymentEvent> captor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(paymentEventRepository).insertIfAbsent(captor.capture());
        assertEquals("PAYMENT_SUCCEEDED", captor.getValue().getEventType());
        assertEquals("PAYMENT_ORDER", captor.getValue().getAggregateType());
        assertEquals("P1", captor.getValue().getAggregateId());
        assertTrue(captor.getValue().getPayload().contains("\"channelTradeNo\":\"T1\""));
        verify(applicationEventPublisher).publishEvent(any(PaymentEventCreatedSignal.class));
    }

    @Test
    void createPaymentSucceededEventDoesNotPublishSignalWhenAlreadyExists() {
        // 模拟 Notify 和主动 Query 几乎同时确认成功：第二次 insertIfAbsent 命中唯一约束返回 false，
        // 不能产生第二次实时投递触发（数据库层面本来就只会有一条事件记录）
        when(paymentEventRepository.insertIfAbsent(any())).thenReturn(false);

        service.createPaymentSucceededEvent(successOrder(), "T1");

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void createPaymentClosedEventUsesCorrectAggregateType() {
        when(paymentEventRepository.insertIfAbsent(any())).thenReturn(true);
        PaymentOrder closedOrder = successOrder().setStatus("CLOSED").setCloseTime(new Date());

        service.createPaymentClosedEvent(closedOrder);

        ArgumentCaptor<PaymentEvent> captor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(paymentEventRepository).insertIfAbsent(captor.capture());
        assertEquals("PAYMENT_CLOSED", captor.getValue().getEventType());
    }

    @Test
    void markFailedAndScheduleRetryIncrementsRetryCountAndSchedulesBackoff() {
        PaymentEvent event = new PaymentEvent().setId(1L).setEventId("E1").setStatus("PUBLISHING").setRetryCount(0);

        service.markFailedAndScheduleRetry(event, "boom");

        assertEquals(1, event.getRetryCount());
        assertEquals("FAILED", event.getStatus());
        assertNotNull(event.getNextRetryTime());
        assertTrue(event.getNextRetryTime().after(new Date()));
        verify(paymentEventRepository).update(event);
    }

    @Test
    void markPublishedSetsStatusAndPublishedAt() {
        PaymentEvent event = new PaymentEvent().setId(1L).setEventId("E1").setStatus("PUBLISHING");

        service.markPublished(event);

        assertEquals("PUBLISHED", event.getStatus());
        assertNotNull(event.getPublishedAt());
        verify(paymentEventRepository).update(event);
    }

    @Test
    void claimForDeliveryDelegatesWithCurrentStatusAsExpected() {
        PaymentEvent event = new PaymentEvent().setId(1L).setStatus(PaymentEventStatus.PENDING.name());
        when(paymentEventRepository.claim(1L, PaymentEventStatus.PENDING, PaymentEventStatus.PUBLISHING)).thenReturn(true);

        assertTrue(service.claimForDelivery(event));
    }
}
