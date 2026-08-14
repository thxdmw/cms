package com.thx.module.payment.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 创建退款请求。appCode+businessRefundNo 是幂等键，支持对同一笔支付多次部分退款。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRefundCommand {
    private String appCode;
    private String paymentNo;
    private String businessRefundNo;
    private BigDecimal amount;
    private String reason;
}
