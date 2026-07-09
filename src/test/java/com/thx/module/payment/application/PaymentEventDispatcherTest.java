package com.thx.module.payment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thx.module.payment.api.event.PaymentEventHandler;
import com.thx.module.payment.api.event.PaymentSucceededEvent;
import com.thx.module.payment.domain.PaymentEvent;
import com.thx.module.payment.domain.PaymentEventType;
import com.thx.module.payment.repository.PaymentEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 事件投递测试：反射扫描 {@code @PaymentEventHandler}、成功调用后标记 PUBLISHED、
 * Handler 抛异常后标记 FAILED 并安排重试、未被任何 Handler 订阅时视为投递成功、
 * CAS 声明失败（被其它调度实例抢先）时不重复处理。
 */
@ExtendWith(MockitoExtension.class)
class PaymentEventDispatcherTest {

    @Mock
    private PaymentEventRepository paymentEventRepository;
    @Mock
    private PaymentEventService paymentEventService;

    private PaymentEventDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new PaymentEventDispatcher(paymentEventRepository, paymentEventService, new ObjectMapper());
    }

    /** 测试用的业务 Listener：模拟 Pet 模块订阅 PAYMENT_SUCCEEDED */
    static class RecordingHandler {
        PaymentSucceededEvent received;
        boolean shouldThrow;

        @PaymentEventHandler(appCode = "PET_APP", eventType = PaymentEventType.PAYMENT_SUCCEEDED)
        public void handle(PaymentSucceededEvent event) {
            if (shouldThrow) {
                throw new IllegalStateException("模拟业务处理失败");
            }
            this.received = event;
        }
    }

    private void scanBean(Object bean) {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getParent()).thenReturn(null);
        when(context.getBeanDefinitionNames()).thenReturn(new String[]{"recordingHandler"});
        doReturn(bean.getClass()).when(context).getType("recordingHandler");
        when(context.getBean("recordingHandler")).thenReturn(bean);
        dispatcher.onApplicationEvent(new ContextRefreshedEvent(context));
    }

    private void scanEmpty() {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getParent()).thenReturn(null);
        when(context.getBeanDefinitionNames()).thenReturn(new String[0]);
        dispatcher.onApplicationEvent(new ContextRefreshedEvent(context));
    }

    private PaymentEvent pendingEvent() {
        return new PaymentEvent().setId(1L).setEventId("E1").setAppCode("PET_APP")
                .setEventType(PaymentEventType.PAYMENT_SUCCEEDED.name())
                .setAggregateType("PAYMENT_ORDER").setAggregateId("P1")
                .setPayload("{\"eventId\":\"E1\",\"paymentNo\":\"P1\",\"businessOrderNo\":\"B1\"}")
                .setStatus("PENDING").setRetryCount(0);
    }

    @Test
    void invokesMatchingHandlerAndMarksPublished() {
        RecordingHandler handlerBean = new RecordingHandler();
        scanBean(handlerBean);
        PaymentEvent event = pendingEvent();
        when(paymentEventService.claimForDelivery(event)).thenReturn(true);

        dispatcher.tryDispatch(event);

        assertNotNull(handlerBean.received);
        assertEquals("P1", handlerBean.received.getPaymentNo());
        assertEquals("E1", handlerBean.received.getEventId());
        verify(paymentEventService).markPublished(event);
        verify(paymentEventService, never()).markFailedAndScheduleRetry(any(), anyString());
    }

    @Test
    void marksFailedAndSchedulesRetryWhenHandlerThrows() {
        RecordingHandler handlerBean = new RecordingHandler();
        handlerBean.shouldThrow = true;
        scanBean(handlerBean);
        PaymentEvent event = pendingEvent();
        when(paymentEventService.claimForDelivery(event)).thenReturn(true);

        dispatcher.tryDispatch(event);

        verify(paymentEventService).markFailedAndScheduleRetry(eq(event), anyString());
        verify(paymentEventService, never()).markPublished(any());
    }

    @Test
    void marksPublishedWhenNoHandlerSubscribed() {
        scanEmpty();
        PaymentEvent event = pendingEvent();
        when(paymentEventService.claimForDelivery(event)).thenReturn(true);

        dispatcher.tryDispatch(event);

        verify(paymentEventService).markPublished(event);
    }

    @Test
    void skipsWhenClaimFailsBecauseAnotherInstanceAlreadyClaimedIt() {
        PaymentEvent event = pendingEvent();
        when(paymentEventService.claimForDelivery(event)).thenReturn(false);

        dispatcher.tryDispatch(event);

        verify(paymentEventService, never()).markPublished(any());
        verify(paymentEventService, never()).markFailedAndScheduleRetry(any(), anyString());
    }
}
