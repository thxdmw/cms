package com.thx.module.payment.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.core.util.StrUtil;
import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.event.PaymentClosedEvent;
import com.thx.module.payment.api.event.PaymentSucceededEvent;
import com.thx.module.payment.api.event.RefundSucceededEvent;
import com.thx.module.payment.domain.PaymentEvent;
import com.thx.module.payment.domain.PaymentEventStatus;
import com.thx.module.payment.domain.PaymentEventType;
import com.thx.module.payment.domain.PaymentOrder;
import com.thx.module.payment.domain.RefundOrder;
import com.thx.module.payment.exception.PaymentErrorCode;
import com.thx.module.payment.exception.PaymentException;
import com.thx.module.payment.infrastructure.PaymentNoGenerator;
import com.thx.module.payment.repository.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

/**
 * 支付事件 Outbox 的创建与生命周期管理。创建类方法（{@code createXxxEvent}）必须运行在调用方
 * 已经开启的事务内（{@code PaymentChannelResultProcessor}/{@code RefundApplicationService}），
 * 与订单/退款状态更新同事务提交；投递类方法（claim/markXxx）供 {@code PaymentEventDispatcher} 使用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventService {

    /** 投递失败重试退避时间表：1分钟、5分钟、30分钟、2小时、6小时、24小时，之后维持 24 小时 */
    private static final long[] BACKOFF_MINUTES = {1, 5, 30, 120, 360, 1440};

    private static final int MAX_ERROR_LENGTH = 1000;

    private final PaymentEventRepository paymentEventRepository;
    private final PaymentNoGenerator noGenerator;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void createPaymentSucceededEvent(PaymentOrder order, String channelTradeNo) {
        PaymentSucceededEvent payload = PaymentSucceededEvent.builder()
                .eventId(noGenerator.nextEventId())
                .paymentNo(order.getPaymentNo())
                .businessOrderNo(order.getBusinessOrderNo())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .channel(PaymentChannel.valueOf(order.getChannel()))
                .channelTradeNo(channelTradeNo)
                .metadata(parseMetadata(order.getMetadata()))
                .successTime(order.getSuccessTime())
                .build();
        createEvent(order.getAppCode(), PaymentEventType.PAYMENT_SUCCEEDED, "PAYMENT_ORDER",
                order.getPaymentNo(), payload.getEventId(), payload);
    }

    public void createPaymentClosedEvent(PaymentOrder order) {
        PaymentClosedEvent payload = PaymentClosedEvent.builder()
                .eventId(noGenerator.nextEventId())
                .paymentNo(order.getPaymentNo())
                .businessOrderNo(order.getBusinessOrderNo())
                .closeTime(order.getCloseTime())
                .build();
        createEvent(order.getAppCode(), PaymentEventType.PAYMENT_CLOSED, "PAYMENT_ORDER",
                order.getPaymentNo(), payload.getEventId(), payload);
    }

    public void createRefundSucceededEvent(RefundOrder refundOrder) {
        RefundSucceededEvent payload = RefundSucceededEvent.builder()
                .eventId(noGenerator.nextEventId())
                .refundNo(refundOrder.getRefundNo())
                .paymentNo(refundOrder.getPaymentNo())
                .businessRefundNo(refundOrder.getBusinessRefundNo())
                .amount(refundOrder.getAmount())
                .currency(refundOrder.getCurrency())
                .successTime(refundOrder.getSuccessTime())
                .build();
        createEvent(refundOrder.getAppCode(), PaymentEventType.REFUND_SUCCEEDED, "REFUND_ORDER",
                refundOrder.getRefundNo(), payload.getEventId(), payload);
    }

    private void createEvent(String appCode, PaymentEventType eventType, String aggregateType,
                              String aggregateId, String eventId, Object payload) {
        PaymentEvent event = new PaymentEvent()
                .setEventId(eventId)
                .setAppCode(appCode)
                .setEventType(eventType.name())
                .setAggregateType(aggregateType)
                .setAggregateId(aggregateId)
                .setPayload(writeJson(payload))
                .setStatus(PaymentEventStatus.PENDING.name())
                .setRetryCount(0);
        boolean inserted = paymentEventRepository.insertIfAbsent(event);
        if (inserted) {
            applicationEventPublisher.publishEvent(new PaymentEventCreatedSignal(eventId));
        }
    }

    /**
     * CAS 声明事件用于投递，PENDING 或到期的 FAILED 均可被声明。
     */
    public boolean claimForDelivery(PaymentEvent event) {
        PaymentEventStatus expected = PaymentEventStatus.valueOf(event.getStatus());
        return paymentEventRepository.claim(event.getId(), expected, PaymentEventStatus.PUBLISHING);
    }

    public void markPublished(PaymentEvent event) {
        event.setStatus(PaymentEventStatus.PUBLISHED.name());
        event.setPublishedAt(new Date());
        paymentEventRepository.update(event);
    }

    public void markFailedAndScheduleRetry(PaymentEvent event, String errorMessage) {
        int retryCount = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
        event.setRetryCount(retryCount);
        event.setStatus(PaymentEventStatus.FAILED.name());
        event.setNextRetryTime(nextRetryTime(retryCount));
        paymentEventRepository.update(event);
        log.warn("支付事件投递失败，已安排重试: eventId={}, retryCount={}, nextRetryTime={}, error={}",
                event.getEventId(), retryCount, event.getNextRetryTime(), truncate(errorMessage));
    }

    private Date nextRetryTime(int retryCount) {
        int index = Math.min(retryCount - 1, BACKOFF_MINUTES.length - 1);
        return Date.from(Instant.now().plus(BACKOFF_MINUTES[index], ChronoUnit.MINUTES));
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_INTERNAL_ERROR, "支付事件序列化失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (StrUtil.isBlank(metadataJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("解析 metadata JSON 失败，返回空: {}", metadataJson, e);
            return null;
        }
    }
}
