package com.thx.module.payment.application;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 事务内发布的轻量信号，携带 eventId；{@code PaymentEventDispatcher} 用
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} 监听，保证只在事务真正提交后才触发投递。
 * 不是对外事件契约，纯粹是进程内实时触发的管道。
 */
@Getter
@RequiredArgsConstructor
public class PaymentEventCreatedSignal {
    private final String eventId;
}
