package com.thx.module.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付模块配置属性，前缀 cms.payment，与项目其它模块（cms.file-system 等）保持同样的命名习惯。
 */
@Data
@ConfigurationProperties(prefix = "cms.payment")
public class PaymentProperties {

    /**
     * 对外可达的回调基地址，用于拼接渠道异步通知 URL，如 https://api.example.com。
     * 必须是渠道服务器能访问到的公网地址，不能是内网/容器内地址。
     */
    private String publicBaseUrl;

    private Reconcile reconcile = new Reconcile();

    private Event event = new Event();

    @Data
    public static class Reconcile {
        /** 是否启用状态补偿定时任务 */
        private boolean enabled = true;
        /** PROCESSING 状态的 Attempt 停留超过该分钟数才纳入主动查询范围 */
        private int processingStaleMinutes = 10;
        /** 每次扫描批量大小 */
        private int batchSize = 50;
    }

    @Data
    public static class Event {
        /** 是否启用事件补偿投递定时任务 */
        private boolean enabled = true;
        /** 每次扫描批量大小 */
        private int batchSize = 50;
    }
}
