package com.thx.module.payment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thx.module.payment.api.event.PaymentClosedEvent;
import com.thx.module.payment.api.event.PaymentEventHandler;
import com.thx.module.payment.api.event.PaymentSucceededEvent;
import com.thx.module.payment.api.event.RefundSucceededEvent;
import com.thx.module.payment.domain.PaymentEvent;
import com.thx.module.payment.domain.PaymentEventType;
import com.thx.module.payment.repository.PaymentEventRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支付事件投递：启动时反射扫描全部 Bean 中标注了 {@link PaymentEventHandler} 的方法
 * （做法类似项目已有的 {@code AnonymousPathScanner} 扫描 {@code @AnonymousAccess}），
 * 事务提交后通过 {@link PaymentEventCreatedSignal} 实时触发投递，{@code PaymentEventScheduler}
 * 定时兜底重投。Payment 模块本身不 import 任何业务模块的类。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventDispatcher implements ApplicationListener<ContextRefreshedEvent> {

    private final PaymentEventRepository paymentEventRepository;
    private final PaymentEventService paymentEventService;
    private final ObjectMapper objectMapper;

    private final Map<HandlerKey, List<HandlerInvoker>> handlers = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        ApplicationContext context = event.getApplicationContext();
        Map<HandlerKey, List<HandlerInvoker>> scanned = new HashMap<>();
        for (String beanName : context.getBeanDefinitionNames()) {
            Class<?> type;
            try {
                type = context.getType(beanName);
            } catch (Exception e) {
                continue;
            }
            if (type == null) {
                continue;
            }
            Class<?> userClass = ClassUtils.getUserClass(type);
            for (Method method : userClass.getMethods()) {
                PaymentEventHandler annotation = method.getAnnotation(PaymentEventHandler.class);
                if (annotation == null) {
                    continue;
                }
                Object bean = context.getBean(beanName);
                HandlerKey key = new HandlerKey(annotation.appCode(), annotation.eventType());
                scanned.computeIfAbsent(key, k -> new ArrayList<>()).add(new HandlerInvoker(bean, method));
            }
        }
        handlers.clear();
        handlers.putAll(scanned);
        log.info("已扫描到 {} 组支付事件处理器", handlers.size());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventCreated(PaymentEventCreatedSignal signal) {
        PaymentEvent event = paymentEventRepository.findByEventId(signal.getEventId());
        if (event != null) {
            tryDispatch(event);
        }
    }

    /**
     * 尝试投递一个事件：CAS 声明 -&gt; 反射调用匹配的 Handler -&gt; 标记结果。
     * 供实时触发和 {@code PaymentEventScheduler} 共用。
     */
    public void tryDispatch(PaymentEvent event) {
        if (!paymentEventService.claimForDelivery(event)) {
            return;
        }
        try {
            dispatchToHandlers(event);
            paymentEventService.markPublished(event);
        } catch (Exception e) {
            paymentEventService.markFailedAndScheduleRetry(event, e.getMessage());
        }
    }

    private void dispatchToHandlers(PaymentEvent event) throws Exception {
        PaymentEventType eventType = PaymentEventType.valueOf(event.getEventType());
        HandlerKey key = new HandlerKey(event.getAppCode(), eventType);
        List<HandlerInvoker> invokers = handlers.get(key);
        if (invokers == null || invokers.isEmpty()) {
            log.debug("没有业务模块订阅该事件，视为投递成功: appCode={}, eventType={}", event.getAppCode(), eventType);
            return;
        }
        Object payload = deserializePayload(event, eventType);
        for (HandlerInvoker invoker : invokers) {
            invoker.invoke(payload);
        }
    }

    private Object deserializePayload(PaymentEvent event, PaymentEventType eventType) throws Exception {
        switch (eventType) {
            case PAYMENT_SUCCEEDED:
                return objectMapper.readValue(event.getPayload(), PaymentSucceededEvent.class);
            case PAYMENT_CLOSED:
                return objectMapper.readValue(event.getPayload(), PaymentClosedEvent.class);
            case REFUND_SUCCEEDED:
                return objectMapper.readValue(event.getPayload(), RefundSucceededEvent.class);
            default:
                throw new IllegalStateException("未知的支付事件类型: " + eventType);
        }
    }

    @Data
    @AllArgsConstructor
    private static class HandlerKey {
        private String appCode;
        private PaymentEventType eventType;
    }

    @RequiredArgsConstructor
    private static class HandlerInvoker {
        private final Object bean;
        private final Method method;

        void invoke(Object payload) throws Exception {
            try {
                method.invoke(bean, payload);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                }
                throw e;
            }
        }
    }
}
