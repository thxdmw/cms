SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- GameSave V2 业务表。文件二进制与生命周期统一委托 module.file。

DROP TABLE IF EXISTS `game_sync_head`;
DROP TABLE IF EXISTS `game_snapshot_file`;
DROP TABLE IF EXISTS `game_snapshot`;
DROP TABLE IF EXISTS `game_object`;
DROP TABLE IF EXISTS `game_library`;
DROP TABLE IF EXISTS `game_device`;
DROP TABLE IF EXISTS `game_account`;

CREATE TABLE `game_account` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id` varchar(64) NOT NULL,
    `username` varchar(64) NOT NULL,
    `password_hash` varchar(255) NOT NULL,
    `quota_bytes` bigint(20) NOT NULL DEFAULT 10737418240 COMMENT '用户逻辑配额，默认 10GB',
    `status` tinyint(4) NOT NULL DEFAULT 1,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_account_user_id` (`user_id`),
    UNIQUE KEY `uk_game_account_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GameSave 用户';

CREATE TABLE `game_device` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `device_id` varchar(64) NOT NULL,
    `user_id` varchar(64) NOT NULL,
    `device_name` varchar(128) NOT NULL,
    `token_hash` varchar(64) NOT NULL COMMENT '设备 Token SHA-256，禁止保存明文',
    `last_seen_time` datetime DEFAULT NULL,
    `status` tinyint(4) NOT NULL DEFAULT 1,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_device_device_id` (`device_id`),
    KEY `idx_game_device_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GameSave 设备';

CREATE TABLE `game_library` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `game_id` varchar(64) NOT NULL,
    `user_id` varchar(64) NOT NULL,
    `game_key` varchar(128) NOT NULL,
    `name` varchar(255) NOT NULL,
    `provider` varchar(32) NOT NULL DEFAULT 'CUSTOM',
    `provider_game_id` varchar(128) DEFAULT NULL,
    `cover_file_id` varchar(64) DEFAULT NULL,
    `status` tinyint(4) NOT NULL DEFAULT 1,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_library_game_id` (`game_id`),
    UNIQUE KEY `uk_game_library_user_key` (`user_id`, `game_key`),
    KEY `idx_game_library_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户逻辑游戏库';

CREATE TABLE `game_object` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `object_id` varchar(64) NOT NULL,
    `user_id` varchar(64) NOT NULL,
    `sha256` varchar(64) NOT NULL,
    `size` bigint(20) NOT NULL,
    `file_id` varchar(64) NOT NULL COMMENT 'module.file file_asset.file_id',
    `reference_count` bigint(20) NOT NULL DEFAULT 0,
    `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_object_object_id` (`object_id`),
    UNIQUE KEY `uk_game_object_user_hash_size` (`user_id`, `sha256`, `size`),
    UNIQUE KEY `uk_game_object_file_id` (`file_id`),
    KEY `idx_game_object_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户级内容寻址对象';

CREATE TABLE `game_snapshot` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `snapshot_id` varchar(64) NOT NULL,
    `user_id` varchar(64) NOT NULL,
    `game_id` varchar(64) NOT NULL,
    `device_id` varchar(64) NOT NULL,
    `parent_snapshot_id` varchar(64) DEFAULT NULL,
    `trigger_type` varchar(32) NOT NULL COMMENT 'MANUAL/GAME_EXIT/BEFORE_RESTORE/IMPORT',
    `description` varchar(500) DEFAULT NULL,
    `file_count` int(11) NOT NULL,
    `logical_size` bigint(20) NOT NULL,
    `changed_file_count` int(11) NOT NULL DEFAULT 0,
    `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_snapshot_snapshot_id` (`snapshot_id`),
    KEY `idx_game_snapshot_game_time` (`user_id`, `game_id`, `create_time`),
    KEY `idx_game_snapshot_parent` (`parent_snapshot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变游戏存档快照';

CREATE TABLE `game_snapshot_file` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `snapshot_id` varchar(64) NOT NULL,
    `relative_path` varchar(1024) NOT NULL,
    `object_id` varchar(64) NOT NULL,
    `size` bigint(20) NOT NULL,
    `sha256` varchar(64) NOT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_snapshot_file_path` (`snapshot_id`, `relative_path`(512)),
    KEY `idx_game_snapshot_file_object` (`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快照文件清单';

CREATE TABLE `game_sync_head` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id` varchar(64) NOT NULL,
    `game_id` varchar(64) NOT NULL,
    `head_snapshot_id` varchar(64) DEFAULT NULL,
    `version` bigint(20) NOT NULL DEFAULT 0,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_sync_head_user_game` (`user_id`, `game_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏同步 HEAD，使用 CAS 推进';

-- 文件系统接入方与策略。生产环境必须替换 API Key Hash；GameSave 内部调用不向客户端分发该 Key。
INSERT INTO `file_app` (`app_id`, `app_name`, `api_key_hash`, `scopes`, `quota_bytes`, `status`)
VALUES ('game-save', 'GameSave 存档同步', '89c12731b6962a35aa475d64a1e4f661228c3e62852c9407a482bcc9e5fc924a',
        'UPLOAD,READ,DELETE,LIST,PRESIGN', NULL, 1)
ON DUPLICATE KEY UPDATE `app_name` = VALUES(`app_name`), `status` = VALUES(`status`);

INSERT INTO `file_policy` (`policy_code`, `max_file_size`, `allowed_extensions`, `allowed_mime_types`, `access_level`, `bucket`, `checksum_required`, `status`)
VALUES ('GAME_SAVE_OBJECT', 536870912, NULL, NULL, 'OWNER_ONLY', 'game-save-private', 1, 1)
ON DUPLICATE KEY UPDATE `max_file_size` = VALUES(`max_file_size`), `access_level` = VALUES(`access_level`), `bucket` = VALUES(`bucket`), `status` = VALUES(`status`);

INSERT INTO `file_app_namespace` (`app_id`, `namespace`, `policy_code`, `status`)
VALUES ('game-save', 'save-object', 'GAME_SAVE_OBJECT', 1)
ON DUPLICATE KEY UPDATE `policy_code` = VALUES(`policy_code`), `status` = VALUES(`status`);

SET FOREIGN_KEY_CHECKS = 1;
