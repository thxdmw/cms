SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 支付模块（Payment）-安装脚本
-- 包含 9 张核心表：
--   payment_business_app / payment_channel_account / payment_app_channel_binding /
--   payment_order / payment_attempt / payment_refund_order /
--   payment_channel_notify_record / payment_event / payment_audit_log
-- 设计说明见 docs/payment-architecture.md。
-- 本脚本按 MySQL 5.7 兼容语法编写（生产环境固定 5.7），不使用 8.0 专属特性。
-- 与 file_system.sql 不同，本模块当前没有真实业务方接入（项目尚无 pet/agent 等业务模块），
-- 因此不包含种子数据；文末以注释形式给出业务方接入时需要执行的 INSERT 示例。
-- ============================================

-- ============================================
-- payment_business_app：支付业务方（调用方应用）
-- ============================================
DROP TABLE IF EXISTS `payment_business_app`;
CREATE TABLE `payment_business_app`
(
    `id`                       bigint(20)    NOT NULL AUTO_INCREMENT,
    `app_code`                 varchar(64)   NOT NULL COMMENT '业务方标识，如 PET_APP、AGENT_PLATFORM，全局唯一',
    `app_name`                 varchar(128)  NOT NULL COMMENT '业务方名称',
    `enabled`                  tinyint(4)    NOT NULL DEFAULT 1 COMMENT '1-启用，0-禁用',
    `webhook_url`               varchar(500)  DEFAULT NULL COMMENT '拆分为独立 Payment Center 后的事件回调地址，当前阶段未使用',
    `webhook_secret_encrypted` varchar(1000) DEFAULT NULL COMMENT 'Webhook 签名密钥密文（AES-GCM），当前阶段未使用',
    `create_time`              datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`              datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_code` (`app_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付业务方（调用方应用）表';

-- ============================================
-- payment_channel_account：支付渠道账号
-- ============================================
DROP TABLE IF EXISTS `payment_channel_account`;
CREATE TABLE `payment_channel_account`
(
    `id`               bigint(20)   NOT NULL AUTO_INCREMENT,
    `account_code`     varchar(64)  NOT NULL COMMENT '渠道账号编码，如 alipay-main，全局唯一',
    `channel`          varchar(32)  NOT NULL COMMENT 'ALIPAY / WECHAT_PAY / STRIPE / PAYPAL',
    `account_name`     varchar(128) NOT NULL COMMENT '账号名称，便于人工识别',
    `config_encrypted` text        NOT NULL COMMENT '渠道配置密文（AES-GCM，主密钥来自环境变量 PAYMENT_MASTER_KEY），禁止落库明文',
    `enabled`          tinyint(4)  NOT NULL DEFAULT 1 COMMENT '1-启用，0-禁用',
    `create_time`      datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_code` (`account_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付渠道账号表';

-- ============================================
-- payment_app_channel_binding：业务方到渠道账号的绑定
-- ============================================
DROP TABLE IF EXISTS `payment_app_channel_binding`;
CREATE TABLE `payment_app_channel_binding`
(
    `id`                 bigint(20)  NOT NULL AUTO_INCREMENT,
    `app_code`           varchar(64) NOT NULL,
    `channel`            varchar(32) NOT NULL,
    `scene`              varchar(16) NOT NULL COMMENT 'APP / H5 / WEB / QR',
    `channel_account_id` bigint(20)  NOT NULL,
    `enabled`            tinyint(4)  NOT NULL DEFAULT 1 COMMENT '1-启用，0-禁用',
    `priority`           int(11)     NOT NULL DEFAULT 0 COMMENT '数值越小优先级越高，同 (app_code,channel,scene) 下取可用最小值',
    `create_time`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`        datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_channel_scene_account` (`app_code`, `channel`, `scene`, `channel_account_id`),
    KEY `idx_app_channel_scene_priority` (`app_code`, `channel`, `scene`, `priority`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='业务方到支付渠道账号的绑定表';

-- ============================================
-- payment_order：支付订单（业务支付意图）
-- ============================================
DROP TABLE IF EXISTS `payment_order`;
CREATE TABLE `payment_order`
(
    `id`                 bigint(20)     NOT NULL AUTO_INCREMENT,
    `payment_no`         varchar(32)    NOT NULL COMMENT '支付模块生成的唯一支付单号',
    `app_code`           varchar(64)    NOT NULL,
    `business_order_no`  varchar(128)   NOT NULL COMMENT '业务方自己的订单号',
    `subject`            varchar(256)   NOT NULL,
    `description`        varchar(512)   DEFAULT NULL,
    `amount`             decimal(18, 2) NOT NULL,
    `currency`           varchar(8)     NOT NULL DEFAULT 'CNY',
    `channel`            varchar(32)    NOT NULL,
    `scene`              varchar(16)    NOT NULL,
    `status`             varchar(32)    NOT NULL COMMENT 'CREATED/PROCESSING/UNKNOWN/SUCCESS/FAILED/CLOSED/PARTIALLY_REFUNDED/REFUNDED',
    `expire_time`        datetime       DEFAULT NULL,
    `success_time`       datetime       DEFAULT NULL,
    `close_time`         datetime       DEFAULT NULL,
    `refunded_amount`    decimal(18, 2) NOT NULL DEFAULT '0.00',
    `metadata`           json           DEFAULT NULL COMMENT '业务方自定义透传数据，如 productId/userId',
    `version`            int(11)        NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`        datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`        datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    UNIQUE KEY `uk_app_business_order` (`app_code`, `business_order_no`),
    KEY `idx_status_create_time` (`status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付订单表';

-- ============================================
-- payment_attempt：一次具体渠道支付尝试
-- ============================================
DROP TABLE IF EXISTS `payment_attempt`;
CREATE TABLE `payment_attempt`
(
    `id`                 bigint(20)   NOT NULL AUTO_INCREMENT,
    `attempt_no`         varchar(32)  NOT NULL COMMENT '渠道调用尝试号，作为提交给渠道的商户订单号(out_trade_no)',
    `payment_no`         varchar(32)  NOT NULL,
    `channel`            varchar(32)  NOT NULL,
    `scene`              varchar(16)  NOT NULL,
    `channel_account_id` bigint(20)   NOT NULL,
    `channel_trade_no`   varchar(64)  DEFAULT NULL COMMENT '渠道侧交易号，如支付宝 trade_no',
    `status`             varchar(32)  NOT NULL COMMENT 'INIT/PROCESSING/UNKNOWN/SUCCESS/FAILED/CLOSED',
    `channel_request`    json         DEFAULT NULL COMMENT '脱敏后的渠道请求快照，禁止包含私钥/证书/完整签名',
    `channel_response`   json         DEFAULT NULL COMMENT '脱敏后的渠道响应快照',
    `failure_code`       varchar(64)  DEFAULT NULL,
    `failure_message`    varchar(500) DEFAULT NULL,
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_attempt_no` (`attempt_no`),
    KEY `idx_payment_no` (`payment_no`),
    KEY `idx_status` (`status`),
    KEY `idx_channel_trade_no` (`channel`, `channel_trade_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付渠道调用尝试表';

-- ============================================
-- payment_refund_order：退款单
-- ============================================
DROP TABLE IF EXISTS `payment_refund_order`;
CREATE TABLE `payment_refund_order`
(
    `id`                  bigint(20)     NOT NULL AUTO_INCREMENT,
    `refund_no`           varchar(32)    NOT NULL,
    `payment_no`          varchar(32)    NOT NULL,
    `app_code`            varchar(64)    NOT NULL,
    `business_refund_no`  varchar(128)   NOT NULL COMMENT '业务方自己的退款单号',
    `amount`              decimal(18, 2) NOT NULL,
    `currency`            varchar(8)     NOT NULL DEFAULT 'CNY',
    `reason`              varchar(256)   DEFAULT NULL,
    `status`              varchar(32)    NOT NULL COMMENT 'INIT/PROCESSING/UNKNOWN/SUCCESS/FAILED/CLOSED',
    `channel_refund_no`   varchar(64)    DEFAULT NULL,
    `failure_code`        varchar(64)    DEFAULT NULL,
    `failure_message`     varchar(500)   DEFAULT NULL,
    `version`             int(11)        NOT NULL DEFAULT 0,
    `success_time`        datetime       DEFAULT NULL,
    `create_time`         datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_no` (`refund_no`),
    UNIQUE KEY `uk_app_business_refund` (`app_code`, `business_refund_no`),
    KEY `idx_payment_no` (`payment_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='退款单表';

-- ============================================
-- payment_channel_notify_record：渠道异步通知记录
-- ============================================
DROP TABLE IF EXISTS `payment_channel_notify_record`;
CREATE TABLE `payment_channel_notify_record`
(
    `id`                  bigint(20)   NOT NULL AUTO_INCREMENT,
    `channel`             varchar(32)  NOT NULL,
    `channel_account_id`  bigint(20)   NOT NULL,
    `notify_key`          varchar(128) NOT NULL COMMENT '幂等键：channel+channelAccountId+channelTradeNo+tradeStatus',
    `payment_no`          varchar(32)  DEFAULT NULL,
    `channel_trade_no`    varchar(64)  DEFAULT NULL,
    `raw_payload`         json         NOT NULL COMMENT '原始通知参数（通知本身是渠道签名断言，不含我方私钥等敏感信息，可安全留存）',
    `signature_verified`  tinyint(4)   NOT NULL DEFAULT 0,
    `process_status`      varchar(32)  NOT NULL COMMENT 'RECEIVED/PROCESSED/REJECTED',
    `process_result`      varchar(500) DEFAULT NULL,
    `received_at`         datetime     NOT NULL,
    `processed_at`        datetime     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notify_key` (`notify_key`),
    KEY `idx_payment_no` (`payment_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='渠道异步通知记录表';

-- ============================================
-- payment_event：支付事件 Outbox
-- ============================================
DROP TABLE IF EXISTS `payment_event`;
CREATE TABLE `payment_event`
(
    `id`              bigint(20)  NOT NULL AUTO_INCREMENT,
    `event_id`        varchar(64) NOT NULL,
    `app_code`        varchar(64) NOT NULL,
    `event_type`      varchar(32) NOT NULL COMMENT 'PAYMENT_SUCCEEDED/PAYMENT_CLOSED/REFUND_SUCCEEDED',
    `aggregate_type`  varchar(32) NOT NULL COMMENT 'PAYMENT_ORDER/REFUND_ORDER',
    `aggregate_id`    varchar(64) NOT NULL COMMENT 'paymentNo 或 refundNo',
    `payload`         json        NOT NULL,
    `status`          varchar(32) NOT NULL COMMENT 'PENDING/PUBLISHING/PUBLISHED/FAILED',
    `retry_count`     int(11)     NOT NULL DEFAULT 0,
    `next_retry_time` datetime    DEFAULT NULL,
    `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `published_at`    datetime    DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    UNIQUE KEY `uk_aggregate_event` (`aggregate_type`, `aggregate_id`, `event_type`),
    KEY `idx_status_next_retry` (`status`, `next_retry_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付事件 Outbox 表';

-- ============================================
-- payment_audit_log：支付审计日志
-- ============================================
DROP TABLE IF EXISTS `payment_audit_log`;
CREATE TABLE `payment_audit_log`
(
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT,
    `app_code`    varchar(64)  DEFAULT NULL,
    `payment_no`  varchar(32)  DEFAULT NULL,
    `attempt_no`  varchar(32)  DEFAULT NULL,
    `refund_no`   varchar(32)  DEFAULT NULL,
    `action`      varchar(32)  NOT NULL COMMENT 'PAYMENT_CREATED/PAYMENT_ATTEMPT_CREATED/PAYMENT_PROCESSING/PAYMENT_UNKNOWN/PAYMENT_SUCCEEDED/PAYMENT_FAILED/PAYMENT_CLOSED/REFUND_CREATED/REFUND_SUCCEEDED/PAYMENT_EVENT_CREATED/PAYMENT_EVENT_PUBLISHED/PAYMENT_EVENT_FAILED',
    `detail`      json         DEFAULT NULL COMMENT '已脱敏的上下文信息',
    `request_id`  varchar(64)  DEFAULT NULL,
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_payment_no` (`payment_no`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付审计日志表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 业务方接入示例（非种子数据，按需人工执行）
-- 新业务方接入 Payment 时的标准步骤：
-- 1) 在 payment_business_app 登记 appCode；
-- 2) 在 payment_channel_account 登记（或复用既有）支付宝商户配置，
--    config_encrypted 必须是应用侧用 PAYMENT_MASTER_KEY 通过 AesGcmCipher 加密后的密文，
--    不能直接写 SQL 明文，因此这里不给出可执行 INSERT，只给出字段示例：
--    account_code=alipay-main, channel=ALIPAY,
--    明文 JSON 示例（加密前）：
--    {"appId":"20210xxxxxxxxxx","gatewayUrl":"https://openapi.alipay.com/gateway.do",
--     "signType":"RSA2","charset":"UTF-8","format":"json","mode":"PUBLIC_KEY",
--     "privateKey":"...","alipayPublicKey":"..."}
-- 3) 在 payment_app_channel_binding 建立绑定：
--    INSERT INTO payment_app_channel_binding
--      (app_code, channel, scene, channel_account_id, enabled, priority)
--    VALUES ('PET_APP', 'ALIPAY', 'APP', <上一步 payment_channel_account.id>, 1, 0);
-- ============================================
