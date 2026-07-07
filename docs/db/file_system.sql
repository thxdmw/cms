SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 文件系统模块-安装脚本
-- 包含 6 张核心表：
--   file_app / file_policy / file_app_namespace /
--   file_asset / storage_cleanup_task / file_operation_log
-- ============================================

-- ============================================
-- file_app：使用文件系统的应用
-- ============================================
DROP TABLE IF EXISTS `file_app`;
CREATE TABLE `file_app`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT,
    `app_id`       varchar(64)  NOT NULL COMMENT '应用标识，如 cms、pet-app、game-a',
    `app_name`     varchar(128) NOT NULL COMMENT '应用名称',
    `api_key_hash` varchar(128) NOT NULL COMMENT 'API Key 的 SHA-256 哈希，禁止保存明文',
    `scopes`       varchar(500) NOT NULL COMMENT '逗号分隔的 Scope 列表，如 UPLOAD,READ,DELETE,LIST,PRESIGN',
    `quota_bytes`  bigint(20)   DEFAULT NULL COMMENT '配额（字节），为空表示不限制',
    `status`       tinyint(4)   NOT NULL DEFAULT 1 COMMENT '1-启用，0-禁用',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_id` (`app_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='文件系统应用表';

-- ============================================
-- file_policy：文件策略（大小/扩展名/MIME/访问级别/Bucket）
-- ============================================
DROP TABLE IF EXISTS `file_policy`;
CREATE TABLE `file_policy`
(
    `id`                 bigint(20)    NOT NULL AUTO_INCREMENT,
    `policy_code`        varchar(64)   NOT NULL COMMENT '策略编码，如 PUBLIC_IMAGE、PRIVATE_FILE',
    `max_file_size`      bigint(20)    NOT NULL COMMENT '最大文件大小（字节）',
    `allowed_extensions` varchar(1000) DEFAULT NULL COMMENT '逗号分隔扩展名，为空表示不限制',
    `allowed_mime_types` varchar(1000) DEFAULT NULL COMMENT '逗号分隔 MIME 类型，为空表示不限制',
    `access_level`       varchar(32)   NOT NULL COMMENT 'PUBLIC / APP_INTERNAL / OWNER_ONLY',
    `bucket`             varchar(128)  NOT NULL COMMENT '对应的存储桶',
    `checksum_required`  tinyint(4)    NOT NULL DEFAULT 1 COMMENT '1-要求校验 SHA256',
    `status`             tinyint(4)    NOT NULL DEFAULT 1 COMMENT '1-启用，0-禁用',
    `create_time`        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_policy_code` (`policy_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='文件策略表';

-- ============================================
-- file_app_namespace：App 业务场景到 Policy 的映射
-- ============================================
DROP TABLE IF EXISTS `file_app_namespace`;
CREATE TABLE `file_app_namespace`
(
    `id`          bigint(20)  NOT NULL AUTO_INCREMENT,
    `app_id`      varchar(64) NOT NULL,
    `namespace`   varchar(64) NOT NULL COMMENT '业务场景，如 article-image、attachment、save',
    `policy_code` varchar(64) NOT NULL,
    `status`      tinyint(4)  NOT NULL DEFAULT 1 COMMENT '1-启用，0-禁用',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_namespace` (`app_id`, `namespace`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='App 业务场景到文件策略的映射表';

-- ============================================
-- file_asset：文件资产元数据（不保存 url / presigned_url，动态生成）
-- ============================================
DROP TABLE IF EXISTS `file_asset`;
CREATE TABLE `file_asset`
(
    `id`                 bigint(20)    NOT NULL AUTO_INCREMENT,
    `file_id`            varchar(64)   NOT NULL COMMENT '文件唯一标识（UUID）',
    `app_id`             varchar(64)   NOT NULL,
    `namespace`          varchar(64)   NOT NULL,
    `policy_code`        varchar(64)   NOT NULL,
    `original_name`      varchar(255)  NOT NULL,
    `extension`          varchar(32)   DEFAULT NULL,
    `content_type`       varchar(128)  DEFAULT NULL COMMENT '客户端声明的 Content-Type',
    `detected_mime_type` varchar(128)  DEFAULT NULL COMMENT 'Tika 检测出的真实 MIME 类型',
    `size`               bigint(20)    NOT NULL,
    `sha256`             varchar(64)   DEFAULT NULL,
    `storage_provider`   varchar(32)   NOT NULL DEFAULT 'MINIO',
    `bucket`             varchar(128)  NOT NULL,
    `object_key`         varchar(512)  NOT NULL,
    `etag`               varchar(255)  DEFAULT NULL COMMENT '对象存储返回的 ETag，注意 ETag != SHA256',
    `access_level`       varchar(32)   NOT NULL COMMENT 'PUBLIC / APP_INTERNAL / OWNER_ONLY',
    `owner_type`         varchar(32)   DEFAULT NULL,
    `owner_id`           varchar(64)   DEFAULT NULL,
    `status`             varchar(32)   NOT NULL COMMENT 'ACTIVE/DELETED/PURGING/PURGED/PURGE_FAILED',
    `deleted_at`         datetime      DEFAULT NULL,
    `create_time`        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_id` (`file_id`),
    UNIQUE KEY `uk_bucket_object` (`storage_provider`, `bucket`, `object_key`),
    KEY `idx_app_status_create` (`app_id`, `status`, `create_time`),
    KEY `idx_app_namespace` (`app_id`, `namespace`),
    KEY `idx_app_sha256` (`app_id`, `sha256`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='文件资产元数据表';

-- ============================================
-- storage_cleanup_task：对象存储清理补偿任务
-- ============================================
DROP TABLE IF EXISTS `storage_cleanup_task`;
CREATE TABLE `storage_cleanup_task`
(
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT,
    `file_id`         varchar(64)  DEFAULT NULL,
    `bucket`          varchar(128) NOT NULL,
    `object_key`      varchar(512) NOT NULL,
    `task_type`       varchar(32)  NOT NULL COMMENT 'DELETE_OBJECT / CLEAN_ORPHAN',
    `status`          varchar(32)  NOT NULL COMMENT 'PENDING/PROCESSING/SUCCESS/FAILED',
    `retry_count`     int(11)      NOT NULL DEFAULT 0,
    `next_retry_time` datetime     DEFAULT NULL,
    `last_error`      varchar(1000) DEFAULT NULL,
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='对象存储清理补偿任务表';

-- ============================================
-- file_operation_log：文件操作审计日志
-- ============================================
DROP TABLE IF EXISTS `file_operation_log`;
CREATE TABLE `file_operation_log`
(
    `id`          bigint(20)  NOT NULL AUTO_INCREMENT,
    `app_id`      varchar(64) NOT NULL,
    `user_id`     varchar(64) DEFAULT NULL,
    `file_id`     varchar(64) DEFAULT NULL,
    `operation`   varchar(32) NOT NULL COMMENT 'UPLOAD/READ/LIST/PRESIGN/DELETE/PURGE',
    `result`      varchar(32) NOT NULL COMMENT 'SUCCESS/FAIL',
    `request_id`  varchar(64) DEFAULT NULL,
    `ip`          varchar(64) DEFAULT NULL,
    `error_code`  varchar(64) DEFAULT NULL,
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_file_id` (`file_id`),
    KEY `idx_app_time` (`app_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='文件操作审计日志表';

-- ============================================
-- 种子数据
-- ============================================

-- cms 应用：API Key 明文为占位符 dev-only-placeholder-change-me（仅供本地开发），
-- 生产环境必须重新生成强随机 Key，计算其 SHA-256 后写入 api_key_hash，不要保存明文。
INSERT INTO `file_app` (`app_id`, `app_name`, `api_key_hash`, `scopes`, `quota_bytes`, `status`)
VALUES ('cms', 'CMS 内容管理系统', '89c12731b6962a35aa475d64a1e4f661228c3e62852c9407a482bcc9e5fc924a', 'UPLOAD,READ,DELETE,LIST,PRESIGN', NULL, 1);

INSERT INTO `file_policy` (`policy_code`, `max_file_size`, `allowed_extensions`, `allowed_mime_types`, `access_level`, `bucket`, `checksum_required`, `status`)
VALUES ('PUBLIC_IMAGE', 10485760, 'jpg,jpeg,png,webp', 'image/jpeg,image/png,image/webp', 'PUBLIC', 'public-assets', 1, 1);

INSERT INTO `file_policy` (`policy_code`, `max_file_size`, `allowed_extensions`, `allowed_mime_types`, `access_level`, `bucket`, `checksum_required`, `status`)
VALUES ('PRIVATE_FILE', 104857600, NULL, NULL, 'OWNER_ONLY', 'private-files', 1, 1);

-- admin 模块「服务器文件管理」功能专用策略：500MB 上限（需容纳压缩包），不限扩展名/MIME，
-- APP_INTERNAL（同 App 即可访问，不是 OWNER_ONLY）—— 这个场景要求所有管理员都能看到/管理彼此
-- 上传的文件，不是"只有上传者本人能访问"的个人云盘语义；复用 private-files 桶，不新建 Bucket
INSERT INTO `file_policy` (`policy_code`, `max_file_size`, `allowed_extensions`, `allowed_mime_types`, `access_level`, `bucket`, `checksum_required`, `status`)
VALUES ('SERVER_FILE', 524288000, NULL, NULL, 'APP_INTERNAL', 'private-files', 1, 1);

INSERT INTO `file_app_namespace` (`app_id`, `namespace`, `policy_code`, `status`)
VALUES ('cms', 'article-image', 'PUBLIC_IMAGE', 1);

INSERT INTO `file_app_namespace` (`app_id`, `namespace`, `policy_code`, `status`)
VALUES ('cms', 'attachment', 'PRIVATE_FILE', 1);

INSERT INTO `file_app_namespace` (`app_id`, `namespace`, `policy_code`, `status`)
VALUES ('cms', 'server-file', 'SERVER_FILE', 1);

SET FOREIGN_KEY_CHECKS = 1;
