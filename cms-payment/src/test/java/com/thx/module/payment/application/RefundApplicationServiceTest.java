package com.thx.module.payment.application;

import com.thx.module.payment.api.command.CreateRefundCommand;
import com.thx.module.payment.api.command.QueryRefundCommand;
import com.thx.module.payment.api.result.RefundResult;
import com.thx.module.payment.channel.spi.ChannelQueryRefundResult;
import com.thx.module.payment.channel.spi.ChannelRefundResult;
import com.thx.module.payment.channel.spi.ChannelResultStatus;
import com.thx.module.payment.channel.spi.PaymentChannelProvider;
import com.thx.module.payment.channel.spi.PaymentChannelRouter;
import com.thx.module.payment.domain.PaymentAttempt;
import com.thx.module.payment.domain.PaymentAttemptStatus;
import com.thx.module.payment.domain.PaymentOrder;
import com.thx.module.payment.domain.PaymentStateMachine;
import com.thx.module.payment.domain.PaymentStatus;
import com.thx.module.payment.domain.RefundOrder;
import com.thx.module.payment.domain.RefundStatus;
import com.thx.module.payment.exception.PaymentException;
import com.thx.module.payment.exception.RefundAmountExceededException;
import com.thx.module.payment.infrastructure.PaymentNoGenerator;
import com.thx.module.payment.repository.PaymentAttemptRepository;
import com.thx.module.payment.repository.PaymentOrderRepository;
import com.thx.module.payment.repository.RefundOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 退款流程测试：全额/部分/多次部分退款、超额拒绝、重复退款幂等、订单状态不支持退款拒绝。
 */
@ExtendWith(MockitoExtension.class)
class RefundApplicationServiceTest {

    @Mock
    private PaymentOrderRepository paymentOrderRepository;
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private RefundOrderRepository refundOrderRepository;
    @Mock
    private PaymentChannelRouter channelRouter;
    @Mock
    private PaymentEventService paymentEventService;
    @Mock
    private PaymentAuditLogService auditLogService;
    @Mock
    private PaymentChannelProvider provider;

    private RefundApplicationService service;

    @BeforeEach
    void setUp() {
        service = new RefundApplicationService(paymentOrderRepository, paymentAttemptRepository, refundOrderRepository,
                channelRouter, new PaymentStateMachine(), new PaymentNoGenerator(), paymentEventService, auditLogService);
    }

    private PaymentOrder order(PaymentStatus status, BigDecimal amount, BigDecimal refundedAmount) {
        return new PaymentOrder()
                .setPaymentNo("P1").setAppCode("PET_APP").setBusinessOrderNo("B1")
                .setAmount(amount).setCurrency("CNY").setChannel("ALIPAY").setScene("APP")
                .setStatus(status.name()).setRefundedAmount(refundedAmount).setVersion(0);
    }

    private PaymentAttempt successAttempt() {
        return new PaymentAttempt().setAttemptNo("A1").setPaymentNo("P1").setChannel("ALIPAY").setScene("APP")
                .setChannelAccountId(1L).setStatus(PaymentAttemptStatus.SUCCESS.name());
    }

    private void stubHappyPathCollaborators(PaymentOrder order) {
        when(refundOrderRepository.findByAppCodeAndBusinessRefundNo(eq("PET_APP"), anyString())).thenReturn(null);
        when(paymentOrderRepository.lockByPaymentNo("P1")).thenReturn(order);
        when(paymentAttemptRepository.findSuccessAttempt("P1")).thenReturn(successAttempt());
        when(channelRouter.route(any(), any())).thenReturn(provider);
    }

    @Test
    void fullRefundMarksOrderRefunded() {
        PaymentOrder order = order(PaymentStatus.SUCCESS, new BigDecimal("100.00"), BigDecimal.ZERO);
        stubHappyPathCollaborators(order);
        when(provider.refund(any())).thenReturn(ChannelRefundResult.builder()
                .resultStatus(ChannelResultStatus.SUCCESS).channelRefundNo("T1").build());
        when(refundOrderRepository.casUpdate(anyString(), anyInt(), eq(RefundStatus.SUCCESS), any(), any(), any(), any()))
                .thenReturn(true);
        when(paymentOrderRepository.casUpdate(eq("P1"), eq(0), eq(PaymentStatus.REFUNDED), any(), any(),
                eq(new BigDecimal("100.00")))).thenReturn(true);

        RefundResult result = service.refund(CreateRefundCommand.builder()
                .appCode("PET_APP").paymentNo("P1").businessRefundNo("R-BIZ-1")
                .amount(new BigDecimal("100.00")).reason("整单退款").build());

        assertEquals(RefundStatus.SUCCESS, result.getStatus());
        verify(paymentEventService).createRefundSucceededEvent(any(RefundOrder.class));
        verify(paymentOrderRepository).casUpdate(eq("P1"), eq(0), eq(PaymentStatus.REFUNDED), any(), any(),
                eq(new BigDecimal("100.00")));
    }

    @Test
    void partialRefundMarksOrderPartiallyRefunded() {
        PaymentOrder order = order(PaymentStatus.SUCCESS, new BigDecimal("100.00"), BigDecimal.ZERO);
        stubHappyPathCollaborators(order);
        when(provider.refund(any())).thenReturn(ChannelRefundResult.builder()
                .resultStatus(ChannelResultStatus.SUCCESS).channelRefundNo("T1").build());
        when(refundOrderRepository.casUpdate(anyString(), anyInt(), eq(RefundStatus.SUCCESS), any(), any(), any(), any()))
                .thenReturn(true);
        when(paymentOrderRepository.casUpdate(eq("P1"), eq(0), eq(PaymentStatus.PARTIALLY_REFUNDED), any(), any(),
                eq(new BigDecimal("30.00")))).thenReturn(true);

        RefundResult result = service.refund(CreateRefundCommand.builder()
                .appCode("PET_APP").paymentNo("P1").businessRefundNo("R-BIZ-1")
                .amount(new BigDecimal("30.00")).build());

        assertEquals(RefundStatus.SUCCESS, result.getStatus());
        verify(paymentOrderRepository).casUpdate(eq("P1"), eq(0), eq(PaymentStatus.PARTIALLY_REFUNDED), any(), any(),
                eq(new BigDecimal("30.00")));
    }

    @Test
    void secondPartialRefundAccumulatesOnTopOfFirst() {
        // 已经退过 30，订单当前是 PARTIALLY_REFUNDED；再退 20，应该累计成 50，而不是覆盖成 20
        PaymentOrder order = order(PaymentStatus.PARTIALLY_REFUNDED, new BigDecimal("100.00"), new BigDecimal("30.00"));
        stubHappyPathCollaborators(order);
        when(provider.refund(any())).thenReturn(ChannelRefundResult.builder()
                .resultStatus(ChannelResultStatus.SUCCESS).channelRefundNo("T2").build());
        when(refundOrderRepository.casUpdate(anyString(), anyInt(), eq(RefundStatus.SUCCESS), any(), any(), any(), any()))
                .thenReturn(true);
        when(paymentOrderRepository.casUpdate(eq("P1"), eq(0), eq(PaymentStatus.PARTIALLY_REFUNDED), any(), any(),
                eq(new BigDecimal("50.00")))).thenReturn(true);

        service.refund(CreateRefundCommand.builder()
                .appCode("PET_APP").paymentNo("P1").businessRefundNo("R-BIZ-2")
                .amount(new BigDecimal("20.00")).build());

        verify(paymentOrderRepository).casUpdate(eq("P1"), eq(0), eq(PaymentStatus.PARTIALLY_REFUNDED), any(), any(),
                eq(new BigDecimal("50.00")));
    }

    @Test
    void rejectsRefundExceedingAvailableBalance() {
        PaymentOrder order = order(PaymentStatus.PARTIALLY_REFUNDED, new BigDecimal("100.00"), new BigDecimal("80.00"));
        when(refundOrderRepository.findByAppCodeAndBusinessRefundNo("PET_APP", "R-BIZ-1")).thenReturn(null);
        when(paymentOrderRepository.lockByPaymentNo("P1")).thenReturn(order);

        CreateRefundCommand command = CreateRefundCommand.builder()
                .appCode("PET_APP").paymentNo("P1").businessRefundNo("R-BIZ-1")
                .amount(new BigDecimal("30.00")).build();

        assertThrows(RefundAmountExceededException.class, () -> service.refund(command));
        verify(channelRouter, never()).route(any(), any());
        verify(refundOrderRepository, never()).insert(any());
    }

    @Test
    void rejectsRefundWhenOrderStatusNotRefundable() {
        PaymentOrder order = order(PaymentStatus.PROCESSING, new BigDecimal("100.00"), BigDecimal.ZERO);
        when(refundOrderRepository.findByAppCodeAndBusinessRefundNo("PET_APP", "R-BIZ-1")).thenReturn(null);
        when(paymentOrderRepository.lockByPaymentNo("P1")).thenReturn(order);

        CreateRefundCommand command = CreateRefundCommand.builder()
                .appCode("PET_APP").paymentNo("P1").businessRefundNo("R-BIZ-1")
                .amount(new BigDecimal("10.00")).build();

        assertThrows(PaymentException.class, () -> service.refund(command));
    }

    @Test
    void duplicateBusinessRefundNoReturnsExistingResultWithoutCallingChannel() {
        RefundOrder existing = new RefundOrder().setRefundNo("R1").setPaymentNo("P1")
                .setAmount(new BigDecimal("30.00")).setStatus(RefundStatus.SUCCESS.name());
        when(refundOrderRepository.findByAppCodeAndBusinessRefundNo("PET_APP", "R-BIZ-1")).thenReturn(existing);

        RefundResult result = service.refund(CreateRefundCommand.builder()
                .appCode("PET_APP").paymentNo("P1").businessRefundNo("R-BIZ-1")
                .amount(new BigDecimal("30.00")).build());

        assertEquals("R1", result.getRefundNo());
        verify(paymentOrderRepository, never()).lockByPaymentNo(any());
        verify(channelRouter, never()).route(any(), any());
    }

    @Test
    void queryRefundResolvesProcessingToSuccessAndUpdatesOrder() {
        RefundOrder refundOrder = new RefundOrder().setRefundNo("R1").setPaymentNo("P1")
                .setAppCode("PET_APP").setAmount(new BigDecimal("30.00")).setCurrency("CNY")
                .setStatus(RefundStatus.PROCESSING.name()).setVersion(0);
        when(refundOrderRepository.findByRefundNo("R1")).thenReturn(refundOrder);
        PaymentOrder order = order(PaymentStatus.SUCCESS, new BigDecimal("100.00"), BigDecimal.ZERO);
        when(paymentOrderRepository.lockByPaymentNo("P1")).thenReturn(order);
        when(paymentAttemptRepository.findSuccessAttempt("P1")).thenReturn(successAttempt());
        when(channelRouter.route(any(), any())).thenReturn(provider);
        when(provider.queryRefund(any())).thenReturn(ChannelQueryRefundResult.builder()
                .resultStatus(ChannelResultStatus.SUCCESS).refundAmount(new BigDecimal("30.00")).channelRefundNo("T1").build());
        when(refundOrderRepository.casUpdate(anyString(), anyInt(), eq(RefundStatus.SUCCESS), any(), any(), any(), any()))
                .thenReturn(true);
        when(paymentOrderRepository.casUpdate(eq("P1"), eq(0), eq(PaymentStatus.PARTIALLY_REFUNDED), any(), any(),
                eq(new BigDecimal("30.00")))).thenReturn(true);

        RefundResult result = service.queryRefund(QueryRefundCommand.builder().refundNo("R1").build());

        assertEquals(RefundStatus.SUCCESS, result.getStatus());
        verify(paymentEventService).createRefundSucceededEvent(any(RefundOrder.class));
    }
}
