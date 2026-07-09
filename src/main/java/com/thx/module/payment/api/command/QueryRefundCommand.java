package com.thx.module.payment.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款查询请求。必须提供 refundNo，或 (appCode + businessRefundNo) 二选一。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRefundCommand {
    private String appCode;
    private String refundNo;
    private String businessRefundNo;
}
