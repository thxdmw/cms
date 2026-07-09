package com.thx.module.payment.channel.alipay;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.thx.module.payment.channel.spi.ChannelNotifyCommand;
import com.thx.module.payment.channel.spi.ChannelNotifyResult;
import com.thx.module.payment.channel.spi.ChannelResultStatus;
import com.thx.module.payment.exception.PaymentErrorCode;
import com.thx.module.payment.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝异步通知验签与字段解析，使用官方 SDK {@link AlipaySignature}，不自行实现签名算法。
 */
@Component
@RequiredArgsConstructor
public class AlipayNotifyParser {

    private final AlipayClientFactory clientFactory;

    public ChannelNotifyResult parseAndVerify(ChannelNotifyCommand command) {
        AlipayChannelConfig config = clientFactory.getConfig(command.getChannelAccountId());
        Map<String, String> params = command.getParams();

        boolean verified;
        try {
            verified = AlipaySignature.rsaCheckV1(params, config.getAlipayPublicKey(), config.getCharset(), config.getSignType());
        } catch (AlipayApiException e) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_NOTIFY_VERIFY_FAILED, "支付宝异步通知验签异常", e);
        }

        BigDecimal totalAmount = null;
        String totalAmountStr = params.get("total_amount");
        if (StrUtil.isNotBlank(totalAmountStr)) {
            totalAmount = new BigDecimal(totalAmountStr);
        }

        // seller_id 未纳入身份比对：ChannelAccount 配置当前不保存预期 seller_id，没有可靠的比对基准，
        // 强行比对反而会造成"检查了但检查不对"的假安全感；app_id 是强制且可靠的身份比对依据。
        boolean identityMatched = StrUtil.isNotBlank(config.getAppId()) && config.getAppId().equals(params.get("app_id"));

        return ChannelNotifyResult.builder()
                .signatureVerified(verified)
                .channelIdentityMatched(identityMatched)
                .outTradeNo(params.get("out_trade_no"))
                .channelTradeNo(params.get("trade_no"))
                .resultStatus(mapTradeStatus(params.get("trade_status")))
                .totalAmount(totalAmount)
                .channelAppId(params.get("app_id"))
                .sellerId(params.get("seller_id"))
                .rawParams(params)
                .build();
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
}
