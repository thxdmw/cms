package com.thx.module.payment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 支付审计日志，对应表：payment_audit_log。detail 必须是已脱敏的上下文信息。
 */
@Data
@Accessors(chain = true)
@TableName("payment_audit_log")
public class PaymentAuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appCode;
    private String paymentNo;
    private String attemptNo;
    private String refundNo;

    /** {@link PaymentAuditAction} 枚举名 */
    private String action;

    /** 已脱敏的上下文信息（JSON 文本） */
    private String detail;

    private String requestId;

    private Date createTime;
}
