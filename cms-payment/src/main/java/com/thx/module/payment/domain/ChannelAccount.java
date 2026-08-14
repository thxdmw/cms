package com.thx.module.payment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 支付渠道账号，对应表：payment_channel_account。
 * configEncrypted 是渠道配置（如支付宝 appId/私钥/网关地址）序列化为 JSON 后用
 * {@link com.thx.module.payment.infrastructure.AesGcmCipher} 加密的密文，禁止落库明文。
 */
@Data
@Accessors(chain = true)
@TableName("payment_channel_account")
public class ChannelAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 渠道账号编码，如 alipay-main，全局唯一 */
    private String accountCode;

    /** {@link com.thx.module.payment.api.enums.PaymentChannel} 枚举名 */
    private String channel;

    /** 账号名称，便于人工识别 */
    private String accountName;

    /** 渠道配置密文 */
    private String configEncrypted;

    /** 1-启用，0-禁用 */
    private Integer enabled;

    private Date createTime;
    private Date updateTime;
}
