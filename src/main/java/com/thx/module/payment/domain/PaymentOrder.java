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
 * 支付订单：业务支付意图，对应表：payment_order。
 * 一个 PaymentOrder 在其生命周期内可以有多个 {@link PaymentAttempt}（重试/换渠道）。
 */
@Data
@Accessors(chain = true)
@TableName("payment_order")
public class PaymentOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 支付模块生成的唯一支付单号 */
    private String paymentNo;

    private String appCode;

    /** 业务方自己的订单号，唯一约束 (appCode, businessOrderNo) */
    private String businessOrderNo;

    private String subject;

    private String description;

    private BigDecimal amount;

    private String currency;

    /** {@link com.thx.module.payment.api.enums.PaymentChannel} 枚举名 */
    private String channel;

    /** {@link com.thx.module.payment.api.enums.PaymentScene} 枚举名 */
    private String scene;

    /** {@link PaymentStatus} 枚举名 */
    private String status;

    private Date expireTime;
    private Date successTime;
    private Date closeTime;

    /** 已退款累计金额 */
    private BigDecimal refundedAmount;

    /** 业务方自定义透传数据（JSON 文本），如 productId/userId */
    private String metadata;

    /**
     * 乐观锁版本号。由 {@link com.thx.module.payment.repository.PaymentOrderRepository} 里的原子 UPDATE 语句
     * 手工维护（SET version=version+1 WHERE version=?），未注册 MyBatis-Plus 的 OptimisticLockerInnerInterceptor，
     * 不要依赖 updateById() 自动生效。
     */
    private Integer version;

    private Date createTime;
    private Date updateTime;
}
