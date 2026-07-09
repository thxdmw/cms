package com.thx.module.payment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thx.module.payment.api.command.CreatePaymentCommand;
import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.enums.PaymentScene;
import com.thx.module.payment.api.result.CreatePaymentResult;
import com.thx.module.payment.channel.spi.ChannelCreatePaymentCommand;
import com.thx.module.payment.channel.spi.ChannelCreatePaymentResult;
import com.thx.module.payment.channel.spi.ChannelResultStatus;
import com.thx.module.payment.channel.spi.PaymentChannelProvider;
import com.thx.module.payment.channel.spi.PaymentChannelRouter;
import com.thx.module.payment.config.PaymentProperties;
import com.thx.module.payment.domain.AppChannelBinding;
import com.thx.module.payment.domain.ChannelAccount;
import com.thx.module.payment.domain.PaymentAttempt;
import com.thx.module.payment.domain.PaymentBusinessApp;
import com.thx.module.payment.domain.PaymentOrder;
import com.thx.module.payment.domain.PaymentStatus;
import com.thx.module.payment.exception.PaymentErrorCode;
import com.thx.module.payment.exception.PaymentException;
import com.thx.module.payment.exception.PaymentOrderConflictException;
import com.thx.module.payment.infrastructure.PaymentNoGenerator;
import com.thx.module.payment.repository.AppChannelBindingRepository;
import com.thx.module.payment.repository.ChannelAccountRepository;
import com.thx.module.payment.repository.PaymentAttemptRepository;
import com.thx.module.payment.repository.PaymentBusinessAppRepository;
import com.thx.module.payment.repository.PaymentOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 创建支付流程测试：首次创建、幂等分支（SUCCESS 直接返回 / 金额冲突拒绝 / CLOSED 拒绝）、
 * out_trade_no 必须取 attemptNo（而不是 paymentNo，见 docs/payment-architecture.md 第五节）。
 */
@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceTest {

    @Mock
    private PaymentBusinessAppRepository businessAppRepository;
    @Mock
    private PaymentOrderRepository paymentOrderRepository;
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private AppChannelBindingRepository appChannelBindingRepository;
    @Mock
    private ChannelAccountRepository channelAccountRepository;
    @Mock
    private PaymentChannelRouter channelRouter;
    @Mock
    private PaymentChannelResultProcessor resultProcessor;
    @Mock
    private PaymentSyncService paymentSyncService;
    @Mock
    private PaymentAuditLogService auditLogService;
    @Mock
    private PaymentChannelProvider provider;

    private PaymentApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PaymentApplicationService(businessAppRepository, paymentOrderRepository, paymentAttemptRepository,
                appChannelBindingRepository, channelAccountRepository, channelRouter, resultProcessor, paymentSyncService,
                new PaymentNoGenerator(), auditLogService, new ObjectMapper(), new PaymentProperties());
    }

    private PaymentBusinessApp enabledApp() {
        return new PaymentBusinessApp().setAppCode("PET_APP").setAppName("Pet App").setEnabled(1);
    }

    private CreatePaymentCommand.CreatePaymentCommandBuilder baseCommand() {
        return CreatePaymentCommand.builder()
                .appCode("PET_APP")
                .businessOrderNo("PET202607090001")
                .subject("图片生成额度10次")
                .amount(new BigDecimal("6.00"))
                .currency("CNY")
                .channel(PaymentChannel.ALIPAY)
                .scene(PaymentScene.APP);
    }

    private PaymentOrder existingOrder(PaymentStatus status) {
        return new PaymentOrder()
                .setPaymentNo("P1")
                .setAppCode("PET_APP")
                .setBusinessOrderNo("PET202607090001")
                .setAmount(new BigDecimal("6.00"))
                .setCurrency("CNY")
                .setChannel("ALIPAY")
                .setScene("APP")
                .setStatus(status.name())
                .setRefundedAmount(BigDecimal.ZERO)
                .setVersion(0);
    }

    @Test
    void createsNewOrderAndAttemptUsingAttemptNoAsOutTradeNo() {
        when(businessAppRepository.findByAppCode("PET_APP")).thenReturn(enabledApp());
        when(paymentOrderRepository.findByAppCodeAndBusinessOrderNo("PET_APP", "PET202607090001")).thenReturn(null);
        AppChannelBinding binding = new AppChannelBinding().setChannelAccountId(1L).setEnabled(1).setPriority(0);
        when(appChannelBindingRepository.findBestBinding("PET_APP", PaymentChannel.ALIPAY, PaymentScene.APP)).thenReturn(binding);
        ChannelAccount account = new ChannelAccount().setId(1L).setAccountCode("alipay-main").setChannel("ALIPAY").setEnabled(1);
        when(channelAccountRepository.findById(1L)).thenReturn(account);
        when(channelRouter.route(PaymentChannel.ALIPAY, PaymentScene.APP)).thenReturn(provider);

        ArgumentCaptor<ChannelCreatePaymentCommand> channelCommandCaptor = ArgumentCaptor.forClass(ChannelCreatePaymentCommand.class);
        when(provider.createPayment(channelCommandCaptor.capture())).thenReturn(ChannelCreatePaymentResult.builder()
                .resultStatus(ChannelResultStatus.PROCESSING)
                .payData(Collections.singletonMap("orderStr", "mock-order-str"))
                .rawResponse(Collections.singletonMap("orderStr", "mock-order-str"))
                .build());

        PaymentOrder updatedOrder = new PaymentOrder()
                .setPaymentNo("Pxxxx")
                .setStatus(PaymentStatus.PROCESSING.name())
                .setChannel("ALIPAY")
                .setScene("APP");
        when(resultProcessor.apply(any(), eq(ChannelResultStatus.PROCESSING), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(updatedOrder);

        CreatePaymentResult result = service.createPayment(baseCommand().build());

        ArgumentCaptor<PaymentAttempt> attemptCaptor = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(paymentOrderRepository).insert(any(PaymentOrder.class));
        verify(paymentAttemptRepository).insert(attemptCaptor.capture());
        assertEquals(attemptCaptor.getValue().getAttemptNo(), channelCommandCaptor.getValue().getOutTradeNo());
        assertEquals(PaymentStatus.PROCESSING, result.getStatus());
        assertEquals("mock-order-str", result.getPayData().get("orderStr"));
    }

    @Test
    void returnsExistingResultWhenOrderAlreadySucceededWithoutCallingChannel() {
        when(businessAppRepository.findByAppCode("PET_APP")).thenReturn(enabledApp());
        when(paymentOrderRepository.findByAppCodeAndBusinessOrderNo("PET_APP", "PET202607090001"))
                .thenReturn(existingOrder(PaymentStatus.SUCCESS));

        CreatePaymentResult result = service.createPayment(baseCommand().build());

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals("P1", result.getPaymentNo());
        assertNull(result.getPayData());
        verifyNoInteractions(channelRouter);
        verify(paymentOrderRepository, never()).insert(any());
    }

    @Test
    void throwsConflictWhenAmountDiffersFromExistingOrder() {
        when(businessAppRepository.findByAppCode("PET_APP")).thenReturn(enabledApp());
        when(paymentOrderRepository.findByAppCodeAndBusinessOrderNo("PET_APP", "PET202607090001"))
                .thenReturn(existingOrder(PaymentStatus.PROCESSING));

        CreatePaymentCommand differentAmount = baseCommand().amount(new BigDecimal("8.00")).build();

        assertThrows(PaymentOrderConflictException.class, () -> service.createPayment(differentAmount));
        verify(paymentOrderRepository, never()).insert(any());
    }

    @Test
    void throwsWhenExistingOrderClosed() {
        when(businessAppRepository.findByAppCode("PET_APP")).thenReturn(enabledApp());
        when(paymentOrderRepository.findByAppCodeAndBusinessOrderNo("PET_APP", "PET202607090001"))
                .thenReturn(existingOrder(PaymentStatus.CLOSED));

        CreatePaymentCommand command = baseCommand().build();
        PaymentException e = assertThrows(PaymentException.class, () -> service.createPayment(command));
        assertEquals(PaymentErrorCode.PAYMENT_ORDER_CLOSED, e.getErrorCode());
    }

    @Test
    void throwsWhenExistingOrderFailed() {
        when(businessAppRepository.findByAppCode("PET_APP")).thenReturn(enabledApp());
        when(paymentOrderRepository.findByAppCodeAndBusinessOrderNo("PET_APP", "PET202607090001"))
                .thenReturn(existingOrder(PaymentStatus.FAILED));

        CreatePaymentCommand command = baseCommand().build();
        PaymentException e = assertThrows(PaymentException.class, () -> service.createPayment(command));
        assertEquals(PaymentErrorCode.PAYMENT_ORDER_FAILED, e.getErrorCode());
    }

    @Test
    void throwsWhenAppNotFound() {
        when(businessAppRepository.findByAppCode("PET_APP")).thenReturn(null);
        CreatePaymentCommand command = baseCommand().build();
        PaymentException e = assertThrows(PaymentException.class, () -> service.createPayment(command));
        assertEquals(PaymentErrorCode.PAYMENT_APP_NOT_FOUND, e.getErrorCode());
    }

    @Test
    void throwsWhenAppDisabled() {
        when(businessAppRepository.findByAppCode("PET_APP")).thenReturn(enabledApp().setEnabled(0));
        CreatePaymentCommand command = baseCommand().build();
        PaymentException e = assertThrows(PaymentException.class, () -> service.createPayment(command));
        assertEquals(PaymentErrorCode.PAYMENT_APP_DISABLED, e.getErrorCode());
    }

    @Test
    void rejectsNonPositiveAmount() {
        CreatePaymentCommand command = baseCommand().amount(BigDecimal.ZERO).build();
        assertThrows(PaymentException.class, () -> service.createPayment(command));
        verifyNoInteractions(businessAppRepository);
    }

    @Test
    void rejectsUnsupportedCurrency() {
        when(businessAppRepository.findByAppCode("PET_APP")).thenReturn(enabledApp());
        when(paymentOrderRepository.findByAppCodeAndBusinessOrderNo("PET_APP", "PET202607090001")).thenReturn(null);
        CreatePaymentCommand command = baseCommand().currency("USD").build();
        PaymentException e = assertThrows(PaymentException.class, () -> service.createPayment(command));
        assertEquals(PaymentErrorCode.PAYMENT_CURRENCY_NOT_SUPPORTED, e.getErrorCode());
    }
}
