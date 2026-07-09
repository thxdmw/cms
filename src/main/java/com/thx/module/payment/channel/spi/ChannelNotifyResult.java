package com.thx.module.payment.channel.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelNotifyResult {
    /** 验签是否通过；验签失败时其余字段仍可能被填充（用于审计记录），但绝不能据此推进订单状态 */
    private boolean signatureVerified;
    /**
     * 渠道身份（如支付宝 app_id）是否与本次使用的 ChannelAccount 配置一致，由 Provider 内部比对得出，
     * Application 层不需要、也不应该知道具体渠道的配置结构。验签通过但身份不匹配同样不能推进订单状态。
     */
    private boolean channelIdentityMatched;
    private String outTradeNo;
    private String channelTradeNo;
    private ChannelResultStatus resultStatus;
    private BigDecimal totalAmount;
    /** 渠道侧应用身份，如支付宝 app_id，供上层校验与 ChannelAccount 配置是否匹配 */
    private String channelAppId;
    /** 卖家/商户身份标识，如支付宝 seller_id，字段存在时用于二次校验 */
    private String sellerId;
    private Map<String, String> rawParams;
}
