package com.thx.module.payment.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付查询请求。必须提供 paymentNo，或 (appCode + businessOrderNo) 二选一，优先按 paymentNo 查询。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryPaymentCommand {
    private String appCode;
    private String paymentNo;
    private String businessOrderNo;
}
