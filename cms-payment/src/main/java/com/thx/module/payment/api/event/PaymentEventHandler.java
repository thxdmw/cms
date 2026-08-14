package com.thx.module.payment.api.event;

import com.thx.module.payment.domain.PaymentEventType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务模块订阅支付事件的入口注解。标注在 Spring Bean 的方法上，方法签名必须是
 * {@code void handle(XxxEvent event)}，参数类型与 {@link #eventType} 对应
 * （{@code PAYMENT_SUCCEEDED} -&gt; {@link PaymentSucceededEvent}，
 * {@code PAYMENT_CLOSED} -&gt; {@link PaymentClosedEvent}，
 * {@code REFUND_SUCCEEDED} -&gt; {@link RefundSucceededEvent}）。
 * <p>
 * 扫描注册方式类似项目已有的 {@code @AnonymousAccess} + {@code AnonymousPathScanner}：
 * Payment 模块不知道、也不 import 任何业务模块，只是启动时反射扫描全部 Bean 的方法。
 * 处理方法必须依据事件的 {@code eventId} 做消费幂等，Payment 会在事件投递失败时重复调用。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PaymentEventHandler {

    /** 只接收该 appCode 产生的事件 */
    String appCode();

    PaymentEventType eventType();
}
