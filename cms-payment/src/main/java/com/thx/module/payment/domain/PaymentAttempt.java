package com.thx.module.payment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 一次具体的渠道支付尝试，对应表：payment_attempt。
 * attemptNo 同时也是提交给渠道的商户订单号（如支付宝 out_trade_no），
 * 保证同一 PaymentOrder 下的多次尝试在渠道侧不会因为订单号重复被拒绝。
 */
@Data
@Accessors(chain = true)
@TableName("payment_attempt")
public class PaymentAttempt implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String attemptNo;

    private String paymentNo;

    /** {@link com.thx.module.payment.api.enums.PaymentChannel} 枚举名 */
    private String channel;

    /** {@link com.thx.module.payment.api.enums.PaymentScene} 枚举名 */
    private String scene;

    private Long channelAccountId;

    /** 渠道侧交易号，如支付宝 trade_no */
    private String channelTradeNo;

    /** {@link PaymentAttemptStatus} 枚举名 */
    private String status;

    /** 脱敏后的渠道请求快照（JSON 文本），禁止包含私钥/证书/完整签名 */
    private String channelRequest;

    /** 脱敏后的渠道响应快照（JSON 文本） */
    private String channelResponse;

    private String failureCode;
    private String failureMessage;

    private Date createTime;
    private Date updateTime;
}
