package com.thx.module.payment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 支付业务方（调用方应用），对应表：payment_business_app。
 * appCode 不是支付宝 appId，是"谁在使用 Payment 基础设施"的身份，如 PET_APP、AGENT_PLATFORM。
 */
@Data
@Accessors(chain = true)
@TableName("payment_business_app")
public class PaymentBusinessApp implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务方标识，全局唯一 */
    private String appCode;

    /** 业务方名称 */
    private String appName;

    /** 1-启用，0-禁用 */
    private Integer enabled;

    /** 拆分为独立 Payment Center 后的事件回调地址，当前阶段未使用 */
    private String webhookUrl;

    /** Webhook 签名密钥密文，当前阶段未使用 */
    private String webhookSecretEncrypted;

    private Date createTime;
    private Date updateTime;
}
