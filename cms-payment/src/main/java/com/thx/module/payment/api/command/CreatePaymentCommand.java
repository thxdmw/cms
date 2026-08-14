package com.thx.module.payment.api.command;

import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.enums.PaymentScene;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 创建支付请求。业务模块通过 {@link com.thx.module.payment.api.PaymentFacade#createPayment} 调用，
 * appCode+businessOrderNo 是幂等键，重复提交相同参数返回既有结果，金额不一致会被拒绝。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentCommand {

    private String appCode;

    private String businessOrderNo;

    private String subject;

    private String description;

    private BigDecimal amount;

    /** 未指定时默认为 CNY，当前只支持 CNY */
    private String currency;

    private PaymentChannel channel;

    private PaymentScene scene;

    private Date expireTime;

    /** 业务方自定义透传数据，如 productId/userId，会原样出现在支付成功事件中 */
    private Map<String, Object> metadata;
}
