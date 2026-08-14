package com.thx.module.payment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 支付事件 Outbox 记录，对应表：payment_event。
 * 唯一约束 (aggregateType, aggregateId, eventType) 保证同一笔支付/退款的同类事件只产生一条，
 * 无论触发来源是异步通知还是主动查询，也无论重复多少次。
 */
@Data
@Accessors(chain = true)
@TableName("payment_event")
public class PaymentEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventId;

    private String appCode;

    /** {@link PaymentEventType} 枚举名 */
    private String eventType;

    /** PAYMENT_ORDER / REFUND_ORDER */
    private String aggregateType;

    /** paymentNo 或 refundNo */
    private String aggregateId;

    /** 事件负载（JSON 文本），反序列化为 {@code com.thx.module.payment.api.event.*Event} */
    private String payload;

    /** {@link PaymentEventStatus} 枚举名 */
    private String status;

    private Integer retryCount;

    private Date nextRetryTime;

    private Date createTime;
    private Date updateTime;
    private Date publishedAt;
}
