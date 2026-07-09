package com.thx.module.payment.api;

import com.thx.module.payment.api.command.CreatePaymentCommand;
import com.thx.module.payment.api.command.CreateRefundCommand;
import com.thx.module.payment.api.command.QueryPaymentCommand;
import com.thx.module.payment.api.command.QueryRefundCommand;
import com.thx.module.payment.api.result.CreatePaymentResult;
import com.thx.module.payment.api.result.PaymentResult;
import com.thx.module.payment.api.result.RefundResult;

/**
 * 业务模块调用支付能力的唯一入口。业务模块禁止直接依赖
 * {@code PaymentOrderMapper}/{@code PaymentOrderRepository}/{@code AlipayPaymentProvider} 等内部实现类，
 * 只能注入本接口。
 * <p>
 * 当前实现是 {@code LocalPaymentFacade}（进程内 Java 调用）。未来 Payment 拆分为独立
 * Payment Center 后，只需要把 Spring 容器里的 Bean 换成基于 HTTP/RPC 的 {@code HttpPaymentFacade}，
 * 调用方注入点不需要改动——这正是本接口存在的意义，也是本模块所有 DTO 都设计为与
 * HTTP 无关的纯 POJO（可直接 Jackson 序列化）的原因。
 */
public interface PaymentFacade {

    /**
     * 创建支付。appCode+businessOrderNo 是幂等键，具体幂等策略见 docs/payment-architecture.md 第十二节。
     */
    CreatePaymentResult createPayment(CreatePaymentCommand command);

    /**
     * 仅查询本地记录的支付状态，不触达渠道。
     */
    PaymentResult queryPayment(QueryPaymentCommand command);

    /**
     * 主动向渠道查询最新状态并同步落库后返回，用于澄清 UNKNOWN/长时间 PROCESSING 的订单。
     */
    PaymentResult syncPayment(QueryPaymentCommand command);

    /**
     * 发起退款，支持对同一笔支付的多次部分退款，累计退款金额不超过支付金额。
     */
    RefundResult refund(CreateRefundCommand command);

    /**
     * 查询退款结果，PROCESSING/UNKNOWN 状态会主动向渠道查询澄清。
     */
    RefundResult queryRefund(QueryRefundCommand command);
}
