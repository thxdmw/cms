package com.thx.module.payment.application;

import cn.hutool.core.util.StrUtil;
import com.thx.module.payment.api.PaymentFacade;
import com.thx.module.payment.api.command.CreatePaymentCommand;
import com.thx.module.payment.api.command.CreateRefundCommand;
import com.thx.module.payment.api.command.QueryPaymentCommand;
import com.thx.module.payment.api.command.QueryRefundCommand;
import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.enums.PaymentScene;
import com.thx.module.payment.api.result.CreatePaymentResult;
import com.thx.module.payment.api.result.PaymentResult;
import com.thx.module.payment.api.result.RefundResult;
import com.thx.module.payment.domain.PaymentOrder;
import com.thx.module.payment.domain.PaymentStatus;
import com.thx.module.payment.exception.PaymentErrorCode;
import com.thx.module.payment.exception.PaymentException;
import com.thx.module.payment.exception.PaymentOrderNotFoundException;
import com.thx.module.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * {@link PaymentFacade} 当前实现：进程内 Java 调用，直接委托各 ApplicationService。
 * 未来拆分为独立 Payment Center 后，替换成基于 HTTP/RPC 的 {@code HttpPaymentFacade} 即可，
 * 业务模块的 {@code @Autowired PaymentFacade} 注入点不需要改动。
 */
@Service
@RequiredArgsConstructor
public class LocalPaymentFacade implements PaymentFacade {

    private final PaymentApplicationService paymentApplicationService;
    private final PaymentSyncService paymentSyncService;
    private final RefundApplicationService refundApplicationService;
    private final PaymentOrderRepository paymentOrderRepository;

    @Override
    public CreatePaymentResult createPayment(CreatePaymentCommand command) {
        return paymentApplicationService.createPayment(command);
    }

    @Override
    public PaymentResult queryPayment(QueryPaymentCommand command) {
        return toPaymentResult(resolveOrder(command));
    }

    @Override
    public PaymentResult syncPayment(QueryPaymentCommand command) {
        PaymentOrder order = resolveOrder(command);
        return toPaymentResult(paymentSyncService.syncPayment(order.getPaymentNo()));
    }

    @Override
    public RefundResult refund(CreateRefundCommand command) {
        return refundApplicationService.refund(command);
    }

    @Override
    public RefundResult queryRefund(QueryRefundCommand command) {
        return refundApplicationService.queryRefund(command);
    }

    private PaymentOrder resolveOrder(QueryPaymentCommand command) {
        if (StrUtil.isNotBlank(command.getPaymentNo())) {
            return paymentSyncService.queryPaymentLocal(command.getPaymentNo());
        }
        if (StrUtil.isNotBlank(command.getAppCode()) && StrUtil.isNotBlank(command.getBusinessOrderNo())) {
            PaymentOrder order = paymentOrderRepository.findByAppCodeAndBusinessOrderNo(
                    command.getAppCode(), command.getBusinessOrderNo());
            if (order == null) {
                throw new PaymentOrderNotFoundException("支付订单不存在: appCode=" + command.getAppCode()
                        + ", businessOrderNo=" + command.getBusinessOrderNo());
            }
            return order;
        }
        throw new PaymentException(PaymentErrorCode.PAYMENT_PARAM_INVALID, "必须提供 paymentNo 或 (appCode+businessOrderNo)");
    }

    private PaymentResult toPaymentResult(PaymentOrder order) {
        return PaymentResult.builder()
                .paymentNo(order.getPaymentNo())
                .appCode(order.getAppCode())
                .businessOrderNo(order.getBusinessOrderNo())
                .status(PaymentStatus.valueOf(order.getStatus()))
                .channel(PaymentChannel.valueOf(order.getChannel()))
                .scene(PaymentScene.valueOf(order.getScene()))
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .refundedAmount(order.getRefundedAmount())
                .successTime(order.getSuccessTime())
                .closeTime(order.getCloseTime())
                .build();
    }
}
