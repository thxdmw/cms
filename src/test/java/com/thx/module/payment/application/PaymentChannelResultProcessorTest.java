package com.thx.module.payment.application;

import com.thx.module.payment.channel.spi.ChannelResultStatus;
import com.thx.module.payment.domain.PaymentAttempt;
import com.thx.module.payment.domain.PaymentAttemptStatus;
import com.thx.module.payment.domain.PaymentAuditAction;
import com.thx.module.payment.domain.PaymentOrder;
import com.thx.module.payment.domain.PaymentStateMachine;
import com.thx.module.payment.domain.PaymentStatus;
import com.thx.module.payment.exception.PaymentException;
import com.thx.module.payment.repository.PaymentAttemptRepository;
import com.thx.module.payment.repository.PaymentOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 渠道结果统一处理测试：Notify 和主动 Query 都会调用 {@link PaymentChannelResultProcessor#apply}，
 * 重点覆盖幂等短路、非法转换的静默忽略与审计、以及"不同 channelTradeNo 不能静默覆盖"。
 */
@ExtendWith(MockitoExtension.class)
class PaymentChannelResultProcessorTest {

    @Mock
    private PaymentOrderRepository paymentOrderRepository;
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private PaymentEventService paymentEventService;
    @Mock
    private PaymentAuditLogService auditLogService;

    private PaymentChannelResultProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PaymentChannelResultProcessor(paymentOrderRepository, paymentAttemptRepository,
                new PaymentStateMachine(), paymentEventService, auditLogService);
    }

    private PaymentOrder order(PaymentStatus status) {
        return new PaymentOrder()
                .setPaymentNo("P1").setAppCode("PET_APP").setBusinessOrderNo("B1")
                .setAmount(new BigDecimal("6.00")).setCurrency("CNY")
                .setChannel("ALIPAY").setScene("APP")
                .setStatus(status.name()).setRefundedAmount(BigDecimal.ZERO).setVersion(0);
    }

    private PaymentAttempt attempt(PaymentAttemptStatus status, String channelTradeNo) {
        return new PaymentAttempt()
                .setAttemptNo("A1").setPaymentNo("P1").setChannel("ALIPAY").setScene("APP")
                .setChannelAccountId(1L).setStatus(status.name()).setChannelTradeNo(channelTradeNo);
    }

    @Test
    void appliesSuccessAndCreatesEvent() {
        PaymentOrder order = order(PaymentStatus.PROCESSING);
        PaymentAttempt attempt = attempt(PaymentAttemptStatus.PROCESSING, null);
        when(paymentOrderRepository.lockByPaymentNo("P1")).thenReturn(order);
        when(paymentOrderRepository.casUpdate(eq("P1"), eq(0), eq(PaymentStatus.SUCCESS), any(), any(), any()))
                .thenReturn(true);

        PaymentOrder result = processor.apply(attempt, ChannelResultStatus.SUCCESS, "T1",
                new BigDecimal("6.00"), null, null);

        assertEquals(PaymentStatus.SUCCESS, PaymentStatus.valueOf(result.getStatus()));
        verify(paymentEventService).createPaymentSucceededEvent(any(PaymentOrder.class), eq("T1"));
        verify(auditLogService).record(eq(PaymentAuditAction.PAYMENT_SUCCEEDED), eq("PET_APP"), eq("P1"));
    }

    @Test
    void rejectsAmountMismatch() {
        PaymentOrder order = order(PaymentStatus.PROCESSING);
        PaymentAttempt attempt = attempt(PaymentAttemptStatus.PROCESSING, null);
        when(paymentOrderRepository.lockByPaymentNo("P1")).thenReturn(order);

        assertThrows(PaymentException.class, () -> processor.apply(attempt, ChannelResultStatus.SUCCESS, "T1",
                new BigDecimal("9.99"), null, null));

        verify(paymentOrderRepository, never()).casUpdate(anyString(), anyInt(), any(), any(), any(), any());
        verify(paymentEventService, never()).createPaymentSucceededEvent(any(), any());
    }

    @Test
    void notifyAndQuerySimultaneousSuccessProducesOnlyOneEvent() {
        PaymentOrder processingOrder = order(PaymentStatus.PROCESSING);
        PaymentOrder successOrder = order(PaymentStatus.SUCCESS);
        PaymentAttempt attempt = attempt(PaymentAttemptStatus.PROCESSING, null);
        when(paymentOrderRepository.lockByPaymentNo("P1")).thenReturn(processingOrder).thenReturn(successOrder);
        when(paymentOrderRepository.casUpdate(eq("P1"), eq(0), eq(PaymentStatus.SUCCESS), any(), any(), any()))
                .thenReturn(true);

        // 第一次：模拟异步通知先到，订单从 PROCESSING 变为 SUCCESS
        processor.apply(attempt, ChannelResultStatus.SUCCESS, "T1", new BigDecimal("6.00"), null, null);
        // 第二次：模拟主动查询几乎同时也发现成功，此时数据库里订单已经是 SUCCESS
        PaymentAttempt attemptAfterFirstCall = attempt(PaymentAttemptStatus.SUCCESS, "T1");
        processor.apply(attemptAfterFirstCall, ChannelResultStatus.SUCCESS, "T1", new BigDecimal("6.00"), null, null);

        verify(paymentEventService, times(1)).createPaymentSucceededEvent(any(PaymentOrder.class), eq("T1"));
        verify(paymentOrderRepository, times(1)).casUpdate(anyString(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void ignoresIllegalTransitionFromNonTerminalStateWithoutThrowing() {
        // UNKNOWN 状态下再次收到 PROCESSING（渠道又变回等待支付），不在允许的转换表内，
        // 但 UNKNOWN 不是"高阶不可逆状态"，属于需要审计但不应该让调用方 500 的场景
        PaymentOrder order = order(PaymentStatus.UNKNOWN);
        PaymentAttempt attempt = attempt(PaymentAttemptStatus.UNKNOWN, null);
        when(paymentOrderRepository.lockByPaymentNo("P1")).thenReturn(order);

        PaymentOrder result = processor.apply(attempt, ChannelResultStatus.PROCESSING, null, null, null, null);

        assertEquals(PaymentStatus.UNKNOWN, PaymentStatus.valueOf(result.getStatus()));
        verify(auditLogService).record(eq(PaymentAuditAction.PAYMENT_ANOMALY_DETECTED), eq("PET_APP"), eq("P1"), any());
        verify(paymentOrderRepository, never()).casUpdate(anyString(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void ignoresLateResultWhenOrderAlreadyTerminalHighOrder() {
        // 已经 REFUNDED 之后又收到一个迟到的 CLOSED 结果，应静默忽略，不抛异常
        PaymentOrder order = order(PaymentStatus.REFUNDED);
        PaymentAttempt attempt = attempt(PaymentAttemptStatus.SUCCESS, "T1");
        when(paymentOrderRepository.lockByPaymentNo("P1")).thenReturn(order);

        PaymentOrder result = processor.apply(attempt, ChannelResultStatus.CLOSED, "T1", null, null, null);

        assertEquals(PaymentStatus.REFUNDED, PaymentStatus.valueOf(result.getStatus()));
        verify(paymentOrderRepository, never()).casUpdate(anyString(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void detectsChannelTradeNoMismatchOnTerminalAttemptWithoutOverwriting() {
        PaymentOrder order = order(PaymentStatus.SUCCESS);
        PaymentAttempt attempt = attempt(PaymentAttemptStatus.SUCCESS, "T1");
        when(paymentOrderRepository.lockByPaymentNo("P1")).thenReturn(order);

        PaymentOrder result = processor.apply(attempt, ChannelResultStatus.SUCCESS, "T2", new BigDecimal("6.00"), null, null);

        assertEquals("T1", attempt.getChannelTradeNo());
        assertEquals(PaymentStatus.SUCCESS, PaymentStatus.valueOf(result.getStatus()));
        verify(auditLogService).record(eq(PaymentAuditAction.PAYMENT_ANOMALY_DETECTED), isNull(),
                eq("P1"), eq("A1"), isNull(), any(), isNull());
        verify(paymentAttemptRepository, never()).update(any());
        verify(paymentEventService, never()).createPaymentSucceededEvent(any(), any());
    }
}
