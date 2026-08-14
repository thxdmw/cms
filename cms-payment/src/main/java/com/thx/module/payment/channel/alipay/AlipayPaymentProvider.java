package com.thx.module.payment.channel.alipay;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.thx.common.util.DateUtil;
import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.enums.PaymentScene;
import com.thx.module.payment.channel.spi.ChannelClosePaymentCommand;
import com.thx.module.payment.channel.spi.ChannelClosePaymentResult;
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
import com.thx.module.payment.channel.spi.PaymentChannelProvider;
import com.thx.module.payment.exception.PaymentErrorCode;
import com.thx.module.payment.exception.PaymentException;
import com.thx.module.payment.infrastructure.SensitiveDataMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付宝 Adapter：当前唯一的真实 {@link PaymentChannelProvider} 实现，只支持 APP 支付场景。
 * 签名/协议/HTTP 调用全部委托 {@link AlipayClient}，本类只负责业务字段映射与结果分类。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayPaymentProvider implements PaymentChannelProvider {

    /** App 支付固定 product_code */
    private static final String PRODUCT_CODE_APP_PAY = "QUICK_MSECURITY_PAY";

    /** trade.query 场景下唯一明确代表"交易从未存在"的 sub_code，可判定为 FAILED；其余一律 UNKNOWN */
    private static final String SUB_CODE_TRADE_NOT_EXIST = "ACQ.TRADE_NOT_EXIST";

    /** 各类查询/退款接口共有的"系统繁忙"类 sub_code，代表结果不明确，必须落 UNKNOWN */
    private static final String SUB_CODE_SYSTEM_ERROR = "ACQ.SYSTEM_ERROR";

    private static final String REFUND_STATUS_SUCCESS = "REFUND_SUCCESS";

    private final AlipayClientFactory clientFactory;
    private final AlipayNotifyParser notifyParser;

    @Override
    public PaymentChannel channel() {
        return PaymentChannel.ALIPAY;
    }

    @Override
    public boolean supports(PaymentChannel channel, PaymentScene scene) {
        return channel == PaymentChannel.ALIPAY && scene == PaymentScene.APP;
    }

    @Override
    public ChannelCreatePaymentResult createPayment(ChannelCreatePaymentCommand command) {
        if (command.getScene() != PaymentScene.APP) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_SCENE_NOT_SUPPORTED, "支付宝当前只支持 APP 场景");
        }
        AlipayClient client = clientFactory.getClient(command.getChannelAccountId());

        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        model.setOutTradeNo(command.getOutTradeNo());
        model.setTotalAmount(command.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        model.setSubject(command.getSubject());
        if (StrUtil.isNotBlank(command.getDescription())) {
            model.setBody(command.getDescription());
        }
        if (command.getExpireTime() != null) {
            model.setTimeExpire(DateUtil.getNewFormatDateString(command.getExpireTime()));
        }
        model.setProductCode(PRODUCT_CODE_APP_PAY);

        AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
        request.setBizModel(model);
        if (StrUtil.isNotBlank(command.getNotifyUrl())) {
            request.setNotifyUrl(command.getNotifyUrl());
        }

        try {
            // sdkExecute 是纯本地签名操作（生成客户端调起支付宝 SDK 所需的 orderStr），不发起任何网络调用，
            // 结果只能在 response.getBody() 里取（该响应对象的 orderStr/tradeNo 等结构化字段不会被填充，
            // 是 SDK 为其它历史场景预留的通用响应模型字段，App 支付场景下始终为空，取值前已通过反编译 SDK 源码确认）。
            AlipayTradeAppPayResponse response = client.sdkExecute(request);
            String orderStr = response.getBody();
            Map<String, Object> rawResponse = new LinkedHashMap<>();
            rawResponse.put("orderStr", orderStr);
            return ChannelCreatePaymentResult.builder()
                    .resultStatus(ChannelResultStatus.PROCESSING)
                    .payData(Collections.singletonMap("orderStr", orderStr))
                    .rawResponse(SensitiveDataMasker.mask(rawResponse))
                    .build();
        } catch (AlipayApiException e) {
            // 本地签名失败一定是配置/参数问题（如私钥格式非法），不是网络问题，不落 UNKNOWN。
            log.error("支付宝 App 支付本地签名失败: outTradeNo={}", command.getOutTradeNo(), e);
            return ChannelCreatePaymentResult.builder()
                    .resultStatus(ChannelResultStatus.FAILED)
                    .failureCode(firstNonBlank(e.getErrCode(), "SDK_SIGN_ERROR"))
                    .failureMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public ChannelQueryPaymentResult queryPayment(ChannelQueryPaymentCommand command) {
        AlipayClient client = clientFactory.getClient(command.getChannelAccountId());
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(command.getOutTradeNo());
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizModel(model);

        try {
            AlipayTradeQueryResponse response = client.execute(request);
            Map<String, Object> rawResponse = rawMap(
                    "code", response.getCode(), "msg", response.getMsg(),
                    "subCode", response.getSubCode(), "subMsg", response.getSubMsg(),
                    "tradeStatus", response.getTradeStatus(), "tradeNo", response.getTradeNo());
            if (response.isSuccess()) {
                BigDecimal totalAmount = StrUtil.isNotBlank(response.getTotalAmount())
                        ? new BigDecimal(response.getTotalAmount()) : null;
                return ChannelQueryPaymentResult.builder()
                        .resultStatus(mapTradeStatus(response.getTradeStatus()))
                        .channelTradeNo(response.getTradeNo())
                        .totalAmount(totalAmount)
                        .rawResponse(rawResponse)
                        .build();
            }
            return ChannelQueryPaymentResult.builder()
                    .resultStatus(SUB_CODE_TRADE_NOT_EXIST.equals(response.getSubCode())
                            ? ChannelResultStatus.FAILED : ChannelResultStatus.UNKNOWN)
                    .failureCode(response.getSubCode())
                    .failureMessage(response.getSubMsg())
                    .rawResponse(rawResponse)
                    .build();
        } catch (AlipayApiException e) {
            log.warn("支付宝查询交易调用异常，判定为 UNKNOWN: outTradeNo={}", command.getOutTradeNo(), e);
            return ChannelQueryPaymentResult.builder()
                    .resultStatus(ChannelResultStatus.UNKNOWN)
                    .failureCode(e.getErrCode())
                    .failureMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public ChannelClosePaymentResult closePayment(ChannelClosePaymentCommand command) {
        AlipayClient client = clientFactory.getClient(command.getChannelAccountId());
        AlipayTradeCloseModel model = new AlipayTradeCloseModel();
        model.setOutTradeNo(command.getOutTradeNo());
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        request.setBizModel(model);

        try {
            AlipayTradeCloseResponse response = client.execute(request);
            Map<String, Object> rawResponse = rawMap("code", response.getCode(), "msg", response.getMsg(),
                    "subCode", response.getSubCode(), "subMsg", response.getSubMsg());
            if (response.isSuccess()) {
                return ChannelClosePaymentResult.builder()
                        .resultStatus(ChannelResultStatus.CLOSED)
                        .rawResponse(rawResponse)
                        .build();
            }
            // 关闭失败不代表支付状态明确，可能订单已经支付成功导致无法关闭，必须交给主动查询澄清，不能直接判定失败
            return ChannelClosePaymentResult.builder()
                    .resultStatus(ChannelResultStatus.UNKNOWN)
                    .failureCode(response.getSubCode())
                    .failureMessage(response.getSubMsg())
                    .rawResponse(rawResponse)
                    .build();
        } catch (AlipayApiException e) {
            log.warn("支付宝关闭交易调用异常，判定为 UNKNOWN: outTradeNo={}", command.getOutTradeNo(), e);
            return ChannelClosePaymentResult.builder()
                    .resultStatus(ChannelResultStatus.UNKNOWN)
                    .failureCode(e.getErrCode())
                    .failureMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public ChannelRefundResult refund(ChannelRefundCommand command) {
        AlipayClient client = clientFactory.getClient(command.getChannelAccountId());
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(command.getOutTradeNo());
        model.setRefundAmount(command.getRefundAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        model.setOutRequestNo(command.getOutRequestNo());
        if (StrUtil.isNotBlank(command.getRefundReason())) {
            model.setRefundReason(command.getRefundReason());
        }
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        request.setBizModel(model);

        try {
            AlipayTradeRefundResponse response = client.execute(request);
            Map<String, Object> rawResponse = rawMap("code", response.getCode(), "msg", response.getMsg(),
                    "subCode", response.getSubCode(), "subMsg", response.getSubMsg(),
                    "fundChange", response.getFundChange(), "tradeNo", response.getTradeNo(),
                    "refundFee", response.getRefundFee());
            if (response.isSuccess() && "Y".equals(response.getFundChange())) {
                return ChannelRefundResult.builder()
                        .resultStatus(ChannelResultStatus.SUCCESS)
                        .channelRefundNo(response.getTradeNo())
                        .rawResponse(rawResponse)
                        .build();
            }
            if (response.isSuccess()) {
                // isSuccess=true 但 fund_change 不是 "Y"：不能等同于退款成功，SDK 没抛异常不代表钱真的退了，
                // 交给主动查询确认。
                return ChannelRefundResult.builder()
                        .resultStatus(ChannelResultStatus.UNKNOWN)
                        .failureMessage("fundChange=" + response.getFundChange())
                        .rawResponse(rawResponse)
                        .build();
            }
            // 系统繁忙类错误结果不明确；其余 sub_code 视为支付宝对这笔具体退款请求的明确拒绝
            return ChannelRefundResult.builder()
                    .resultStatus(SUB_CODE_SYSTEM_ERROR.equals(response.getSubCode())
                            ? ChannelResultStatus.UNKNOWN : ChannelResultStatus.FAILED)
                    .failureCode(response.getSubCode())
                    .failureMessage(response.getSubMsg())
                    .rawResponse(rawResponse)
                    .build();
        } catch (AlipayApiException e) {
            log.warn("支付宝退款调用异常，判定为 UNKNOWN: outTradeNo={}, outRequestNo={}",
                    command.getOutTradeNo(), command.getOutRequestNo(), e);
            return ChannelRefundResult.builder()
                    .resultStatus(ChannelResultStatus.UNKNOWN)
                    .failureCode(e.getErrCode())
                    .failureMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public ChannelQueryRefundResult queryRefund(ChannelQueryRefundCommand command) {
        AlipayClient client = clientFactory.getClient(command.getChannelAccountId());
        AlipayTradeFastpayRefundQueryModel model = new AlipayTradeFastpayRefundQueryModel();
        model.setOutTradeNo(command.getOutTradeNo());
        model.setOutRequestNo(command.getOutRequestNo());
        AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
        request.setBizModel(model);

        try {
            AlipayTradeFastpayRefundQueryResponse response = client.execute(request);
            Map<String, Object> rawResponse = rawMap("code", response.getCode(), "msg", response.getMsg(),
                    "subCode", response.getSubCode(), "subMsg", response.getSubMsg(),
                    "refundStatus", response.getRefundStatus(), "refundAmount", response.getRefundAmount());
            if (!response.isSuccess()) {
                return ChannelQueryRefundResult.builder()
                        .resultStatus(ChannelResultStatus.UNKNOWN)
                        .failureCode(response.getSubCode())
                        .failureMessage(response.getSubMsg())
                        .rawResponse(rawResponse)
                        .build();
            }
            // refund_status 字段只有在退款已确认成功时才会出现，未出现不代表失败，只代表尚未确认成功
            if (REFUND_STATUS_SUCCESS.equals(response.getRefundStatus())) {
                BigDecimal refundAmount = StrUtil.isNotBlank(response.getRefundAmount())
                        ? new BigDecimal(response.getRefundAmount()) : null;
                return ChannelQueryRefundResult.builder()
                        .resultStatus(ChannelResultStatus.SUCCESS)
                        .refundAmount(refundAmount)
                        .channelRefundNo(response.getTradeNo())
                        .rawResponse(rawResponse)
                        .build();
            }
            return ChannelQueryRefundResult.builder()
                    .resultStatus(ChannelResultStatus.UNKNOWN)
                    .rawResponse(rawResponse)
                    .build();
        } catch (AlipayApiException e) {
            log.warn("支付宝退款查询调用异常，判定为 UNKNOWN: outTradeNo={}, outRequestNo={}",
                    command.getOutTradeNo(), command.getOutRequestNo(), e);
            return ChannelQueryRefundResult.builder()
                    .resultStatus(ChannelResultStatus.UNKNOWN)
                    .failureCode(e.getErrCode())
                    .failureMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public ChannelNotifyResult parseAndVerifyNotify(ChannelNotifyCommand command) {
        return notifyParser.parseAndVerify(command);
    }

    private ChannelResultStatus mapTradeStatus(String tradeStatus) {
        if (tradeStatus == null) {
            return ChannelResultStatus.UNKNOWN;
        }
        switch (tradeStatus) {
            case "TRADE_SUCCESS":
            case "TRADE_FINISHED":
                return ChannelResultStatus.SUCCESS;
            case "TRADE_CLOSED":
                return ChannelResultStatus.CLOSED;
            case "WAIT_BUYER_PAY":
                return ChannelResultStatus.PROCESSING;
            default:
                return ChannelResultStatus.UNKNOWN;
        }
    }

    private Map<String, Object> rawMap(Object... kvPairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kvPairs.length; i += 2) {
            map.put(String.valueOf(kvPairs[i]), kvPairs[i + 1]);
        }
        return SensitiveDataMasker.mask(map);
    }

    private String firstNonBlank(String a, String b) {
        return StrUtil.isNotBlank(a) ? a : b;
    }
}
