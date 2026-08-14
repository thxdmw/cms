package com.thx.module.payment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 业务方到支付渠道账号的绑定，对应表：payment_app_channel_binding。
 * 创建支付时按 (appCode, channel, scene) 查启用中、priority 最小的一条绑定。
 */
@Data
@Accessors(chain = true)
@TableName("payment_app_channel_binding")
public class AppChannelBinding implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appCode;

    /** {@link com.thx.module.payment.api.enums.PaymentChannel} 枚举名 */
    private String channel;

    /** {@link com.thx.module.payment.api.enums.PaymentScene} 枚举名 */
    private String scene;

    private Long channelAccountId;

    /** 1-启用，0-禁用 */
    private Integer enabled;

    /** 数值越小优先级越高 */
    private Integer priority;

    private Date createTime;
    private Date updateTime;
}
