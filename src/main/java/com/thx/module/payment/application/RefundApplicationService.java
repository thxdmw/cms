package com.thx.module.payment.application;

import cn.hutool.core.util.StrUtil;
import com.thx.module.payment.api.command.CreateRefundCommand;
import com.thx.module.payment.api.command.QueryRefundCommand;
import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.enums.PaymentScene;
import com.thx.module.payment.api.result.RefundResult;
import com.thx.module.payment.channel.spi.ChannelQueryRefundCommand;
import com.thx.module.payment.channel.spi.ChannelQueryRefundResult;
import com.thx.module.payment.channel.spi.ChannelRefundCommand;
import com.thx.module.payment.channel.spi.ChannelRefundResult;
import com.thx.module.payment.channel.spi.ChannelResultStatus;
import com.thx.module.payment.channel.spi.PaymentChannelProvider;
import com.thx.module.payment.channel.spi.PaymentChannelRouter;
import com.thx.module.payment.domain.PaymentAttempt;
import com.thx.module.payment.domain.PaymentAuditAction;
import com.thx.module.payment.domain.PaymentOrder;
import com.thx.module.payment.domain.PaymentStateMachine;
import com.thx.module.payment.domain.PaymentStatus;
import com.thx.module.payment.domain.RefundOrder;
import com.thx.module.payment.domain.RefundStatus;
import com.thx.module.payment.exception.PaymentErrorCode;
import com.thx.module.payment.exception.PaymentException;
import com.thx.module.payment.exception.PaymentOrderNotFoundException;
import com.thx.module.payment.exception.RefundAmountExceededException;
import com.thx.module.payment.infrastructure.PaymentNoGenerator;
import com.thx.module.payment.repository.PaymentAttemptRepository;
import com.thx.module.payment.repository.PaymentOrderRepository;
import com.thx.module.payment.repository.RefundOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 退款流程编排：全额/部分/多次部分退款，并发退款不超额。
 * <p>
 * {@link #refund} 与 {@link PaymentApplicationService#createPayment} 的设计取向不同：
 * 这里刻意让 {@code SELECT ... FOR UPDATE} 行锁持续到渠道退款调用完成才释放，
 * 把"校验可退余额"与"实际调用渠道"合并成一个不可分割的临界区——因为
 * {@code alipay.trade.refund} 官方文档保证是同步、快速、幂等的接口，且退款不是高频热点路径，
 * 用锁的方式换取最简单直接、不需要额外补偿逻辑的强一致性是合理取舍。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundApplicationService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final RefundOrderRepository refundOrderRepository;
    private final PaymentChannelRouter channelRouter;
    private final PaymentStateMachine stateMachine;
    private final PaymentNoGenerator noGenerator;
    private final PaymentEventService paymentEventService;
    private final PaymentAuditLogService auditLogService;

    @Transactional
    public RefundResult refund(CreateRefundCommand command) {
        validateCommand(command);

        RefundOrder existing = refundOrderRepository.findByAppCodeAndBusinessRefundNo(
                command.getAppCode(), command.getBusinessRefundNo());
        if (existing != null) {
            if (existing.getAmount().compareTo(command.getAmount()) != 0
                    || !existing.getPaymentNo().equals(command.getPaymentNo())) {
                throw new PaymentException(PaymentErrorCode.REFUND_ORDER_CONFLICT,
                        "退款单参数冲突: refundNo=" + existing.getRefundNo());
            }
            return toRefundResult(existing);
        }

        PaymentOrder order = paymentOrderRepository.lockByPaymentNo(command.getPaymentNo());
        if (order == null) {
            throw new PaymentOrderNotFoundException("支付订单不存在: paymentNo=" + command.getPaymentNo());
        }
        if (!order.getAppCode().equals(command.getAppCode())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PARAM_INVALID, "appCode 与订单不匹配");
        }
        PaymentStatus orderStatus = PaymentStatus.valueOf(order.getStatus());
        if (orderStatus != PaymentStatus.SUCCESS && orderStatus != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_ILLEGAL_STATE,
                    "订单当前状态不支持退款: paymentNo=" + command.getPaymentNo() + ", status=" + orderStatus);
        }
        BigDecimal newRefundedAmount = order.getRefundedAmount().add(command.getAmount());
        if (newRefundedAmount.compareTo(order.getAmount()) > 0) {
            throw new RefundAmountExceededException("退款金额超过可退余额: paymentNo=" + command.getPaymentNo()
                    + ", 已退=" + order.getRefundedAmount() + ", 本次=" + command.getAmount()
                    + ", 订单金额=" + order.getAmount());
        }
        PaymentStatus targetOrderStatusIfSuccess = newRefundedAmount.compareTo(order.getAmount()) == 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        stateMachine.transition(orderStatus, targetOrderStatusIfSuccess);

        PaymentAttempt successAttempt = paymentAttemptRepository.findSuccessAttempt(order.getPaymentNo());
        if (successAttempt == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_INTERNAL_ERROR,
                    "找不到已成功的支付尝试: paymentNo=" + order.getPaymentNo());
        }

        String refundNo = noGenerator.nextRefundNo();
        RefundOrder refundOrder = new RefundOrder()
                .setRefundNo(refundNo)
                .setPaymentNo(order.getPaymentNo())
                .setAppCode(order.getAppCode())
                .setBusinessRefundNo(command.getBusinessRefundNo())
                .setAmount(command.getAmount())
                .setCurrency(order.getCurrency())
                .setReason(command.getReason())
                .setStatus(RefundStatus.INIT.name())
                .setVersion(0);
        refundOrderRepository.insert(refundOrder);
        auditLogService.record(PaymentAuditAction.REFUND_CREATED, order.getAppCode(), order.getPaymentNo(),
                null, refundNo, null, null);

        PaymentChannelProvider provider = channelRouter.route(
                PaymentChannel.valueOf(order.getChannel()), PaymentScene.valueOf(order.getScene()));
        ChannelRefundCommand channelCommand = ChannelRefundCommand.builder()
                .channelAccountId(successAttempt.getChannelAccountId())
                .outTradeNo(successAttempt.getAttemptNo())
                .refundAmount(command.getAmount())
                .outRequestNo(refundNo)
                .refundReason(command.getReason())
                .build();

        ChannelRefundResult channelResult;
        try {
            channelResult = provider.refund(channelCommand);
        } catch (Exception e) {
            log.error("调用支付渠道退款发生未预期异常，判定为 UNKNOWN: refundNo={}", refundNo, e);
            channelResult = ChannelRefundResult.builder()
                    .resultStatus(ChannelResultStatus.UNKNOWN)
                    .failureCode("UNEXPECTED_ERROR")
                    .failureMessage(e.getMessage())
                    .build();
        }

        return applyRefundResult(refundOrder, order, newRefundedAmount, channelResult.getResultStatus(),
                channelResult.getChannelRefundNo(), channelResult.getFailureCode(), channelResult.getFailureMessage());
    }

    @Transactional
    public RefundResult queryRefund(QueryRefundCommand command) {
        RefundOrder refundOrder = resolveRefundOrder(command);
        if (!isPendingStatus(refundOrder.getStatus())) {
            return toRefundResult(refundOrder);
        }

        PaymentOrder order = paymentOrderRepository.lockByPaymentNo(refundOrder.getPaymentNo());
        if (order == null) {
            throw new PaymentOrderNotFoundException("支付订单不存在: paymentNo=" + refundOrder.getPaymentNo());
        }
        // 重新读取：等待订单行锁期间，可能已被另一个并发的 queryRefund 处理完成
        refundOrder = refundOrderRepository.findByRefundNo(refundOrder.getRefundNo());
        if (!isPendingStatus(refundOrder.getStatus())) {
            return toRefundResult(refundOrder);
        }

        PaymentAttempt successAttempt = paymentAttemptRepository.findSuccessAttempt(order.getPaymentNo());
        if (successAttempt == null) {
            return toRefundResult(refundOrder);
        }
        PaymentChannelProvider provider = channelRouter.route(
                PaymentChannel.valueOf(order.getChannel()), PaymentScene.valueOf(order.getScene()));
        ChannelQueryRefundCommand queryCommand = ChannelQueryRefundCommand.builder()
                .channelAccountId(successAttempt.getChannelAccountId())
                .outTradeNo(successAttempt.getAttemptNo())
                .outRequestNo(refundOrder.getRefundNo())
                .build();

        ChannelQueryRefundResult result;
        try {
            result = provider.queryRefund(queryCommand);
        } catch (Exception e) {
            log.error("查询支付渠道退款结果发生未预期异常，判定为 UNKNOWN: refundNo={}", refundOrder.getRefundNo(), e);
            result = ChannelQueryRefundResult.builder()
                    .resultStatus(ChannelResultStatus.UNKNOWN)
                    .failureCode("UNEXPECTED_ERROR")
                    .failureMessage(e.getMessage())
                    .build();
        }

        BigDecimal newRefundedAmount = order.getRefundedAmount().add(refundOrder.getAmount());
        return applyRefundResult(refundOrder, order, newRefundedAmount, result.getResultStatus(),
                result.getChannelRefundNo(), result.getFailureCode(), result.getFailureMessage());
    }

    private RefundResult applyRefundResult(RefundOrder refundOrder, PaymentOrder order, BigDecimal newRefundedAmount,
                                            ChannelResultStatus channelStatus, String channelRefundNo,
                                            String failureCode, String failureMessage) {
        RefundStatus currentRefundStatus = RefundStatus.valueOf(refundOrder.getStatus());
        RefundStatus targetRefundStatus = mapToRefundStatus(channelStatus);
        if (currentRefundStatus == targetRefundStatus) {
            return toRefundResult(refundOrder);
        }

        Date successTime = targetRefundStatus == RefundStatus.SUCCESS ? new Date() : refundOrder.getSuccessTime();
        boolean refundUpdated = refundOrderRepository.casUpdate(refundOrder.getRefundNo(), refundOrder.getVersion(),
                targetRefundStatus, channelRefundNo, failureCode, failureMessage, successTime);
        if (!refundUpdated) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_INTERNAL_ERROR,
                    "退款单并发更新冲突: refundNo=" + refundOrder.getRefundNo());
        }
        refundOrder.setStatus(targetRefundStatus.name());
        refundOrder.setChannelRefundNo(channelRefundNo);
        refundOrder.setSuccessTime(successTime);

        if (targetRefundStatus == RefundStatus.SUCCESS) {
            PaymentStatus currentOrderStatus = PaymentStatus.valueOf(order.getStatus());
            PaymentStatus newOrderStatus = newRefundedAmount.compareTo(order.getAmount()) == 0
                    ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
            stateMachine.transition(currentOrderStatus, newOrderStatus);
            boolean orderUpdated = paymentOrderRepository.casUpdate(order.getPaymentNo(), order.getVersion(),
                    newOrderStatus, order.getSuccessTime(), order.getCloseTime(), newRefundedAmount);
            if (!orderUpdated) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_INTERNAL_ERROR,
                        "支付订单并发更新冲突: paymentNo=" + order.getPaymentNo());
            }
            paymentEventService.createRefundSucceededEvent(refundOrder);
            auditLogService.record(PaymentAuditAction.REFUND_SUCCEEDED, order.getAppCode(), order.getPaymentNo(),
                    null, refundOrder.getRefundNo(), null, null);
        } else if (targetRefundStatus == RefundStatus.FAILED) {
            auditLogService.record(PaymentAuditAction.REFUND_FAILED, order.getAppCode(), order.getPaymentNo(),
                    null, refundOrder.getRefundNo(), null, null);
        }
        // UNKNOWN/PROCESSING：不修改 PaymentOrder.refundedAmount，等待再次 queryRefund 主动确认

        return toRefundResult(refundOrder);
    }

    private void validateCommand(CreateRefundCommand command) {
        if (StrUtil.isBlank(command.getAppCode())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PARAM_INVALID, "appCode 不能为空");
        }
        if (StrUtil.isBlank(command.getPaymentNo())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PARAM_INVALID, "paymentNo 不能为空");
        }
        if (StrUtil.isBlank(command.getBusinessRefundNo())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PARAM_INVALID, "businessRefundNo 不能为空");
        }
        if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PARAM_INVALID, "amount 必须大于 0");
        }
        if (command.getAmount().scale() > 2) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PARAM_INVALID, "amount 最多支持 2 位小数");
        }
    }

    private RefundOrder resolveRefundOrder(QueryRefundCommand command) {
        RefundOrder refundOrder = null;
        if (StrUtil.isNotBlank(command.getRefundNo())) {
            refundOrder = refundOrderRepository.findByRefundNo(command.getRefundNo());
        } else if (StrUtil.isNotBlank(command.getAppCode()) && StrUtil.isNotBlank(command.getBusinessRefundNo())) {
            refundOrder = refundOrderRepository.findByAppCodeAndBusinessRefundNo(
                    command.getAppCode(), command.getBusinessRefundNo());
        }
        if (refundOrder == null) {
            throw new PaymentException(PaymentErrorCode.REFUND_ORDER_NOT_FOUND, "退款单不存在");
        }
        return refundOrder;
    }

    private boolean isPendingStatus(String status) {
        RefundStatus s = RefundStatus.valueOf(status);
        return s == RefundStatus.INIT || s == RefundStatus.PROCESSING || s == RefundStatus.UNKNOWN;
    }

    private RefundStatus mapToRefundStatus(ChannelResultStatus channelStatus) {
        switch (channelStatus) {
            case SUCCESS:
                return RefundStatus.SUCCESS;
            case FAILED:
                return RefundStatus.FAILED;
            case CLOSED:
                return RefundStatus.CLOSED;
            case PROCESSING:
                return RefundStatus.PROCESSING;
            default:
                return RefundStatus.UNKNOWN;
        }
    }

    private RefundResult toRefundResult(RefundOrder refundOrder) {
        return RefundResult.builder()
                .refundNo(refundOrder.getRefundNo())
                .paymentNo(refundOrder.getPaymentNo())
                .status(RefundStatus.valueOf(refundOrder.getStatus()))
                .amount(refundOrder.getAmount())
                .failureCode(refundOrder.getFailureCode())
                .failureMessage(refundOrder.getFailureMessage())
                .successTime(refundOrder.getSuccessTime())
                .build();
    }
}
