package com.thx.module.payment.channel.alipay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.enums.PaymentScene;
import com.thx.module.payment.channel.spi.ChannelCreatePaymentCommand;
import com.thx.module.payment.channel.spi.ChannelCreatePaymentResult;
import com.thx.module.payment.channel.spi.ChannelNotifyCommand;
import com.thx.module.payment.channel.spi.ChannelNotifyResult;
import com.thx.module.payment.channel.spi.ChannelQueryPaymentCommand;
import com.thx.module.payment.channel.spi.ChannelQueryPaymentResult;
import com.thx.module.payment.channel.spi.ChannelQueryRefundCommand;
import com.thx.module.payment.channel.spi.ChannelQueryRefundResult;
import com.thx.module.payment.channel.spi.ChannelRefundCommand;
import com.thx.module.payment.channel.spi.ChannelRefundResult;
import com.thx.module.payment.channel.spi.ChannelResultStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付宝 Adapter 测试：Mock {@link AlipayClient}，覆盖请求参数映射、App Pay 从 body 取 orderStr、
 * 查询/退款结果解析、超时判定为 UNKNOWN（而不是 FAILED）、SDK 未抛异常但 fund_change!=Y 不算退款成功。
 */
@ExtendWith(MockitoExtension.class)
class AlipayPaymentProviderTest {

    @Mock
    private AlipayClientFactory clientFactory;
    @Mock
    private AlipayNotifyParser notifyParser;
    @Mock
    private AlipayClient alipayClient;

    private AlipayPaymentProvider provider;

    @BeforeEach
    void setUp() {
        provider = new AlipayPaymentProvider(clientFactory, notifyParser);
        // lenient: supportsOnlyAlipayAppCombination/parseAndVerifyNotifyDelegatesToNotifyParser 不需要这个 stub
        lenient().when(clientFactory.getClient(1L)).thenReturn(alipayClient);
    }

    @Test
    void supportsOnlyAlipayAppCombination() {
        assertEquals(PaymentChannel.ALIPAY, provider.channel());
        assertTrue(provider.supports(PaymentChannel.ALIPAY, PaymentScene.APP));
        assertFalse(provider.supports(PaymentChannel.ALIPAY, PaymentScene.H5));
        assertFalse(provider.supports(PaymentChannel.WECHAT_PAY, PaymentScene.APP));
    }

    @Test
    void createPaymentMapsRequestFieldsAndReturnsOrderStrFromBody() throws AlipayApiException {
        AlipayTradeAppPayResponse response = new AlipayTradeAppPayResponse();
        response.setBody("mock-order-str");
        when(alipayClient.sdkExecute(any(AlipayTradeAppPayRequest.class))).thenReturn(response);

        ChannelCreatePaymentCommand command = ChannelCreatePaymentCommand.builder()
                .channelAccountId(1L).scene(PaymentScene.APP).outTradeNo("A1")
                .subject("图片生成额度10次").amount(new BigDecimal("6.00")).currency("CNY")
                .notifyUrl("https://example.com/api/payment/channel-notify/alipay/alipay-main").build();

        ChannelCreatePaymentResult result = provider.createPayment(command);

        assertEquals(ChannelResultStatus.PROCESSING, result.getResultStatus());
        assertEquals("mock-order-str", result.getPayData().get("orderStr"));

        ArgumentCaptor<AlipayTradeAppPayRequest> captor = ArgumentCaptor.forClass(AlipayTradeAppPayRequest.class);
        verify(alipayClient).sdkExecute(captor.capture());
        AlipayTradeAppPayModel model = (AlipayTradeAppPayModel) captor.getValue().getBizModel();
        assertEquals("A1", model.getOutTradeNo());
        assertEquals("6.00", model.getTotalAmount());
        assertEquals("图片生成额度10次", model.getSubject());
        assertEquals("QUICK_MSECURITY_PAY", model.getProductCode());
        assertEquals("https://example.com/api/payment/channel-notify/alipay/alipay-main", captor.getValue().getNotifyUrl());
    }

    @Test
    void createPaymentReturnsFailedWhenLocalSigningThrows() throws AlipayApiException {
        // sdkExecute 是纯本地签名，不涉及网络，异常一定是配置问题，判定为 FAILED 而不是 UNKNOWN
        when(alipayClient.sdkExecute(any())).thenThrow(new AlipayApiException("私钥格式非法"));

        ChannelCreatePaymentCommand command = ChannelCreatePaymentCommand.builder()
                .channelAccountId(1L).scene(PaymentScene.APP).outTradeNo("A1")
                .subject("subject").amount(new BigDecimal("6.00")).currency("CNY").build();

        ChannelCreatePaymentResult result = provider.createPayment(command);

        assertEquals(ChannelResultStatus.FAILED, result.getResultStatus());
    }

    @Test
    void queryPaymentParsesSuccessResponse() throws AlipayApiException {
        AlipayTradeQueryResponse response = new AlipayTradeQueryResponse();
        response.setTradeStatus("TRADE_SUCCESS");
        response.setTradeNo("T1");
        response.setTotalAmount("6.00");
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class))).thenReturn(response);

        ChannelQueryPaymentResult result = provider.queryPayment(ChannelQueryPaymentCommand.builder()
                .channelAccountId(1L).outTradeNo("A1").build());

        assertEquals(ChannelResultStatus.SUCCESS, result.getResultStatus());
        assertEquals("T1", result.getChannelTradeNo());
        assertEquals(0, new BigDecimal("6.00").compareTo(result.getTotalAmount()));
    }

    @Test
    void queryPaymentReturnsUnknownOnTimeoutException() throws AlipayApiException {
        // 核心场景：HTTP Timeout 不能判定为支付失败
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class)))
                .thenThrow(new AlipayApiException(new SocketTimeoutException("timeout")));

        ChannelQueryPaymentResult result = provider.queryPayment(ChannelQueryPaymentCommand.builder()
                .channelAccountId(1L).outTradeNo("A1").build());

        assertEquals(ChannelResultStatus.UNKNOWN, result.getResultStatus());
    }

    @Test
    void queryPaymentReturnsFailedWhenTradeNotExist() throws AlipayApiException {
        AlipayTradeQueryResponse response = new AlipayTradeQueryResponse();
        response.setSubCode("ACQ.TRADE_NOT_EXIST");
        response.setSubMsg("交易不存在");
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class))).thenReturn(response);

        ChannelQueryPaymentResult result = provider.queryPayment(ChannelQueryPaymentCommand.builder()
                .channelAccountId(1L).outTradeNo("A1").build());

        assertEquals(ChannelResultStatus.FAILED, result.getResultStatus());
    }

    @Test
    void queryPaymentReturnsUnknownOnAmbiguousSystemError() throws AlipayApiException {
        AlipayTradeQueryResponse response = new AlipayTradeQueryResponse();
        response.setSubCode("ACQ.SYSTEM_ERROR");
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class))).thenReturn(response);

        ChannelQueryPaymentResult result = provider.queryPayment(ChannelQueryPaymentCommand.builder()
                .channelAccountId(1L).outTradeNo("A1").build());

        assertEquals(ChannelResultStatus.UNKNOWN, result.getResultStatus());
    }

    @Test
    void refundReturnsSuccessOnlyWhenFundChangeIsY() throws AlipayApiException {
        AlipayTradeRefundResponse response = new AlipayTradeRefundResponse();
        response.setFundChange("Y");
        response.setTradeNo("T1");
        when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(response);

        ChannelRefundResult result = provider.refund(ChannelRefundCommand.builder()
                .channelAccountId(1L).outTradeNo("A1").refundAmount(new BigDecimal("6.00")).outRequestNo("R1").build());

        assertEquals(ChannelResultStatus.SUCCESS, result.getResultStatus());
        assertEquals("T1", result.getChannelRefundNo());
    }

    @Test
    void refundReturnsUnknownWhenFundChangeIsNotYEvenThoughCallSucceeded() throws AlipayApiException {
        // 核心场景："SDK 没抛异常" 不等于 "退款成功"，必须看 fund_change
        AlipayTradeRefundResponse response = new AlipayTradeRefundResponse();
        response.setFundChange("N");
        when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(response);

        ChannelRefundResult result = provider.refund(ChannelRefundCommand.builder()
                .channelAccountId(1L).outTradeNo("A1").refundAmount(new BigDecimal("6.00")).outRequestNo("R1").build());

        assertEquals(ChannelResultStatus.UNKNOWN, result.getResultStatus());
    }

    @Test
    void refundReturnsFailedOnBusinessRejection() throws AlipayApiException {
        AlipayTradeRefundResponse response = new AlipayTradeRefundResponse();
        response.setSubCode("ACQ.TRADE_STATUS_ERROR");
        response.setSubMsg("交易状态不合法");
        when(alipayClient.execute(any(AlipayTradeRefundRequest.class))).thenReturn(response);

        ChannelRefundResult result = provider.refund(ChannelRefundCommand.builder()
                .channelAccountId(1L).outTradeNo("A1").refundAmount(new BigDecimal("6.00")).outRequestNo("R1").build());

        assertEquals(ChannelResultStatus.FAILED, result.getResultStatus());
    }

    @Test
    void queryRefundReturnsSuccessWhenRefundStatusPresent() throws AlipayApiException {
        AlipayTradeFastpayRefundQueryResponse response = new AlipayTradeFastpayRefundQueryResponse();
        response.setRefundStatus("REFUND_SUCCESS");
        response.setRefundAmount("6.00");
        when(alipayClient.execute(any(AlipayTradeFastpayRefundQueryRequest.class))).thenReturn(response);

        ChannelQueryRefundResult result = provider.queryRefund(ChannelQueryRefundCommand.builder()
                .channelAccountId(1L).outTradeNo("A1").outRequestNo("R1").build());

        assertEquals(ChannelResultStatus.SUCCESS, result.getResultStatus());
        assertEquals(0, new BigDecimal("6.00").compareTo(result.getRefundAmount()));
    }

    @Test
    void queryRefundReturnsUnknownWhenRefundStatusFieldAbsent() throws AlipayApiException {
        // 支付宝文档：refund_status 只有在确认成功时才出现，不出现不代表失败
        AlipayTradeFastpayRefundQueryResponse response = new AlipayTradeFastpayRefundQueryResponse();
        when(alipayClient.execute(any(AlipayTradeFastpayRefundQueryRequest.class))).thenReturn(response);

        ChannelQueryRefundResult result = provider.queryRefund(ChannelQueryRefundCommand.builder()
                .channelAccountId(1L).outTradeNo("A1").outRequestNo("R1").build());

        assertEquals(ChannelResultStatus.UNKNOWN, result.getResultStatus());
    }

    @Test
    void parseAndVerifyNotifyDelegatesToNotifyParser() {
        ChannelNotifyCommand command = ChannelNotifyCommand.builder()
                .channelAccountId(1L).params(Collections.emptyMap()).build();
        ChannelNotifyResult expected = ChannelNotifyResult.builder().signatureVerified(true).build();
        when(notifyParser.parseAndVerify(command)).thenReturn(expected);

        ChannelNotifyResult result = provider.parseAndVerifyNotify(command);

        assertEquals(expected, result);
    }
}
