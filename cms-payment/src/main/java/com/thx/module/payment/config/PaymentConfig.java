package com.thx.module.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付模块配置装配入口：绑定 {@link PaymentProperties}。
 * {@code @EnableScheduling} 已经在 {@code SpringbootApplication} 全局开启（file 模块的
 * {@code FileCleanupTask} 已在使用），Payment 的补偿 Scheduler 直接复用，不需要重复声明。
 */
@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentConfig {
}
