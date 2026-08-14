package com.thx.module.payment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 渠道异步通知记录，对应表：payment_channel_notify_record。
 * notifyKey 是稳定的幂等键（channel+channelAccountId+channelTradeNo+tradeStatus），
 * 数据库唯一约束防止重复通知重复入库；但最终的支付成功幂等仍以
 * {@link PaymentOrder} 状态机 + {@link PaymentEvent} 唯一约束为准，本记录只是第一道防线。
 */
@Data
@Accessors(chain = true)
@TableName("payment_channel_notify_record")
public class ChannelNotifyRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** {@link com.thx.module.payment.api.enums.PaymentChannel} 枚举名 */
    private String channel;

    private Long channelAccountId;

    private String notifyKey;

    private String paymentNo;

    private String channelTradeNo;

    /** 原始通知参数（JSON 文本） */
    private String rawPayload;

    /** 1-验签通过，0-验签未通过 */
    private Integer signatureVerified;

    /** RECEIVED / PROCESSED / REJECTED */
    private String processStatus;

    private String processResult;

    private Date receivedAt;
    private Date processedAt;
}
