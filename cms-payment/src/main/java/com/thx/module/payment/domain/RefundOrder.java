package com.thx.module.payment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 退款单，对应表：payment_refund_order。支持对同一 PaymentOrder 的多次部分退款，
 * 累计退款金额记录在 {@link PaymentOrder#getRefundedAmount()}。
 */
@Data
@Accessors(chain = true)
@TableName("payment_refund_order")
public class RefundOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private String paymentNo;

    private String appCode;

    /** 业务方自己的退款单号，唯一约束 (appCode, businessRefundNo) */
    private String businessRefundNo;

    private BigDecimal amount;

    private String currency;

    private String reason;

    /** {@link RefundStatus} 枚举名 */
    private String status;

    private String channelRefundNo;

    private String failureCode;
    private String failureMessage;

    /**
     * 乐观锁版本号，与 {@link PaymentOrder#getVersion()} 同理，由
     * {@link com.thx.module.payment.repository.RefundOrderRepository} 手工维护。
     */
    private Integer version;

    private Date successTime;

    private Date createTime;
    private Date updateTime;
}
