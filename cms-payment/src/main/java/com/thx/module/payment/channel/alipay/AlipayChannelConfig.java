package com.thx.module.payment.channel.alipay;

import lombok.Data;

/**
 * {@code ChannelAccount.configEncrypted} 解密后的支付宝配置逻辑模型。
 */
@Data
public class AlipayChannelConfig {

    private String appId;

    /** 支付宝网关地址，如 https://openapi.alipay.com/gateway.do */
    private String gatewayUrl;

    /** 签名算法，固定 RSA2 */
    private String signType;

    private String charset;

    /** 固定 json */
    private String format;

    private AlipayConfigMode mode;

    /** 商户私钥（PUBLIC_KEY 模式使用） */
    private String privateKey;

    /** 支付宝公钥（PUBLIC_KEY 模式使用） */
    private String alipayPublicKey;

    /** 应用公钥证书内容（CERTIFICATE 模式预留，当前未接线） */
    private String appCertContent;

    /** 支付宝公钥证书内容（CERTIFICATE 模式预留，当前未接线） */
    private String alipayCertContent;

    /** 支付宝根证书内容（CERTIFICATE 模式预留，当前未接线） */
    private String alipayRootCertContent;
}
