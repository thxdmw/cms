package com.thx.module.payment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.channel.spi.ChannelNotifyResult;
import com.thx.module.payment.channel.spi.ChannelResultStatus;
import com.thx.module.payment.channel.spi.PaymentChannelProvider;
import com.thx.module.payment.channel.spi.PaymentChannelRouter;
import com.thx.module.payment.domain.ChannelAccount;
import com.thx.module.payment.domain.ChannelNotifyRecord;
import com.thx.module.payment.domain.PaymentAttempt;
import com.thx.module.payment.domain.PaymentOrder;
import com.thx.module.payment.exception.PaymentErrorCode;
import com.thx.module.payment.exception.PaymentException;
import com.thx.module.payment.repository.ChannelAccountRepository;
import com.thx.module.payment.repository.ChannelNotifyRecordRepository;
import com.thx.module.payment.repository.PaymentAttemptRepository;
import com.thx.module.payment.repository.PaymentOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付宝异步通知处理测试：验签失败/身份不匹配拒绝、Attempt/Order 缺失拒绝、
 * 重复通知幂等短路、业务处理失败时记录 REJECTED（而不是永久拒绝后续重试）。
 */
@ExtendWith(MockitoExtension.class)
class PaymentNotifyServiceTest {

    @Mock
    private ChannelAccountRepository channelAccountRepository;
    @Mock
    private PaymentChannelRouter channelRouter;
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private PaymentOrderRepository paymentOrderRepository;
    @Mock
    private ChannelNotifyRecordRepository notifyRecordRepository;
    @Mock
    private PaymentChannelResultProcessor resultProcessor;
    @Mock
    private PaymentChannelProvider provider;

    private PaymentNotifyService service;

    @BeforeEach
    void setUp() {
        service = new PaymentNotifyService(channelAccountRepository, channelRouter, paymentAttemptRepository,
                paymentOrderRepository, notifyRecordRepository, resultProcessor, new ObjectMapper());
    }

    private ChannelAccount enabledAccount() {
        return new ChannelAccount().setId(1L).setAccountCode("alipay-main").setChannel("ALIPAY").setEnabled(1);
    }

    private Map<String, String> rawParams() {
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "A1");
        params.put("trade_no", "T1");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("total_amount", "6.00");
        params.put("app_id", "2021000000000000");
        return params;
    }

    private ChannelNotifyResult verifiedSuccessResult() {
        return ChannelNotifyResult.builder()
                .signatureVerified(true)
                .channelIdentityMatched(true)
                .outTradeNo("A1")
                .channelTradeNo("T1")
                .resultStatus(ChannelResultStatus.SUCCESS)
                .totalAmount(new BigDecimal("6.00"))
                .rawParams(rawParams())
                .build();
    }

    @Test
    void processesValidNotifySuccessfullyAndMarksProcessed() {
        when(channelAccountRepository.findByAccountCode("alipay-main")).thenReturn(enabledAccount());
        when(channelRouter.routeByChannel(PaymentChannel.ALIPAY)).thenReturn(provider);
        when(provider.parseAndVerifyNotify(any())).thenReturn(verifiedSuccessResult());
        PaymentAttempt attempt = new PaymentAttempt().setAttemptNo("A1").setPaymentNo("P1").setStatus("PROCESSING");
        when(paymentAttemptRepository.findByAttemptNo("A1")).thenReturn(attempt);
        when(paymentOrderRepository.findByPaymentNo("P1")).thenReturn(new PaymentOrder().setPaymentNo("P1"));
        when(notifyRecordRepository.findByNotifyKey(any())).thenReturn(null);
        when(notifyRecordRepository.tryInsert(any())).thenReturn(true);
        when(resultProcessor.apply(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PaymentOrder().setPaymentNo("P1").setStatus("SUCCESS"));

        boolean result = service.handleNotify(PaymentChannel.ALIPAY, "alipay-main", rawParams(), "127.0.0.1");

        assertTrue(result);
        ArgumentCaptor<ChannelNotifyRecord> captor = ArgumentCaptor.forClass(ChannelNotifyRecord.class);
        verify(notifyRecordRepository).update(captor.capture());
        assertEquals("PROCESSED", captor.getValue().getProcessStatus());
    }

    @Test
    void rejectsWhenChannelAccountNotFound() {
        when(channelAccountRepository.findByAccountCode("unknown")).thenReturn(null);

        boolean result = service.handleNotify(PaymentChannel.ALIPAY, "unknown", rawParams(), "127.0.0.1");

        assertFalse(result);
        verify(channelRouter, never()).routeByChannel(any());
    }

    @Test
    void rejectsWhenChannelAccountDisabled() {
        when(channelAccountRepository.findByAccountCode("alipay-main")).thenReturn(enabledAccount().setEnabled(0));

        boolean result = service.handleNotify(PaymentChannel.ALIPAY, "alipay-main", rawParams(), "127.0.0.1");

        assertFalse(result);
        verify(channelRouter, never()).routeByChannel(any());
    }

    @Test
    void rejectsWhenSignatureVerificationFails() {
        when(channelAccountRepository.findByAccountCode("alipay-main")).thenReturn(enabledAccount());
        when(channelRouter.routeByChannel(PaymentChannel.ALIPAY)).thenReturn(provider);
        when(provider.parseAndVerifyNotify(any())).thenReturn(ChannelNotifyResult.builder()
                .signatureVerified(false)
                .outTradeNo("A1")
                .build());

        boolean result = service.handleNotify(PaymentChannel.ALIPAY, "alipay-main", rawParams(), "127.0.0.1");

        assertFalse(result);
        verify(paymentAttemptRepository, never()).findByAttemptNo(any());
    }

    @Test
    void rejectsWhenChannelIdentityMismatched() {
        when(channelAccountRepository.findByAccountCode("alipay-main")).thenReturn(enabledAccount());
        when(channelRouter.routeByChannel(PaymentChannel.ALIPAY)).thenReturn(provider);
        when(provider.parseAndVerifyNotify(any())).thenReturn(ChannelNotifyResult.builder()
                .signatureVerified(true)
                .channelIdentityMatched(false)
                .outTradeNo("A1")
                .channelAppId("2099999999999999")
                .build());

        boolean result = service.handleNotify(PaymentChannel.ALIPAY, "alipay-main", rawParams(), "127.0.0.1");

        assertFalse(result);
        verify(paymentAttemptRepository, never()).findByAttemptNo(any());
    }

    @Test
    void rejectsWhenAttemptNotFound() {
        when(channelAccountRepository.findByAccountCode("alipay-main")).thenReturn(enabledAccount());
        when(channelRouter.routeByChannel(PaymentChannel.ALIPAY)).thenReturn(provider);
        when(provider.parseAndVerifyNotify(any())).thenReturn(verifiedSuccessResult());
        when(paymentAttemptRepository.findByAttemptNo("A1")).thenReturn(null);

        boolean result = service.handleNotify(PaymentChannel.ALIPAY, "alipay-main", rawParams(), "127.0.0.1");

        assertFalse(result);
        verify(resultProcessor, never()).apply(any(), any(), any(), any(), any(), any());
    }

    @Test
    void duplicateNotifyShortCircuitsWithoutReprocessing() {
        when(channelAccountRepository.findByAccountCode("alipay-main")).thenReturn(enabledAccount());
        when(channelRouter.routeByChannel(PaymentChannel.ALIPAY)).thenReturn(provider);
        when(provider.parseAndVerifyNotify(any())).thenReturn(verifiedSuccessResult());
        when(paymentAttemptRepository.findByAttemptNo("A1"))
                .thenReturn(new PaymentAttempt().setAttemptNo("A1").setPaymentNo("P1"));
        when(paymentOrderRepository.findByPaymentNo("P1")).thenReturn(new PaymentOrder().setPaymentNo("P1"));
        ChannelNotifyRecord processed = new ChannelNotifyRecord().setProcessStatus("PROCESSED");
        when(notifyRecordRepository.findByNotifyKey(any())).thenReturn(processed);

        boolean result = service.handleNotify(PaymentChannel.ALIPAY, "alipay-main", rawParams(), "127.0.0.1");

        assertTrue(result);
        verify(resultProcessor, never()).apply(any(), any(), any(), any(), any(), any());
        verify(notifyRecordRepository, never()).tryInsert(any());
    }

    @Test
    void businessFailureMarksRejectedButStillReturnsFalseAllowingFutureRetry() {
        when(channelAccountRepository.findByAccountCode("alipay-main")).thenReturn(enabledAccount());
        when(channelRouter.routeByChannel(PaymentChannel.ALIPAY)).thenReturn(provider);
        when(provider.parseAndVerifyNotify(any())).thenReturn(verifiedSuccessResult());
        when(paymentAttemptRepository.findByAttemptNo("A1"))
                .thenReturn(new PaymentAttempt().setAttemptNo("A1").setPaymentNo("P1"));
        when(paymentOrderRepository.findByPaymentNo("P1")).thenReturn(new PaymentOrder().setPaymentNo("P1"));
        when(notifyRecordRepository.findByNotifyKey(any())).thenReturn(null);
        when(notifyRecordRepository.tryInsert(any())).thenReturn(true);
        when(resultProcessor.apply(any(), any(), any(), any(), any(), any()))
                .thenThrow(new PaymentException(PaymentErrorCode.PAYMENT_NOTIFY_AMOUNT_MISMATCH, "金额不匹配"));

        boolean result = service.handleNotify(PaymentChannel.ALIPAY, "alipay-main", rawParams(), "127.0.0.1");

        assertFalse(result);
        ArgumentCaptor<ChannelNotifyRecord> captor = ArgumentCaptor.forClass(ChannelNotifyRecord.class);
        verify(notifyRecordRepository).update(captor.capture());
        assertEquals("REJECTED", captor.getValue().getProcessStatus());
    }
}
