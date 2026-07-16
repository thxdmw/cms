CREATE TABLE IF NOT EXISTS `game_account` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id` varchar(64) NOT NULL,
    `username` varchar(64) NOT NULL,
    `password_hash` varchar(255) NOT NULL,
    `quota_bytes` bigint(20) NOT NULL DEFAULT 10737418240,
    `used_bytes` bigint(20) NOT NULL DEFAULT 0,
    `status` tinyint(4) NOT NULL DEFAULT 1,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_account_user_id` (`user_id`),
    UNIQUE KEY `uk_game_account_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `game_device` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `device_id` varchar(64) NOT NULL,
    `user_id` varchar(64) NOT NULL,
    `device_name` varchar(128) NOT NULL,
    `token_hash` varchar(64) NOT NULL,
    `last_seen_time` datetime DEFAULT NULL,
    `status` tinyint(4) NOT NULL DEFAULT 1,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_device_device_id` (`device_id`),
    UNIQUE KEY `uk_game_device_token_hash` (`token_hash`),
    KEY `idx_game_device_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `game_library` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `game_id` varchar(64) NOT NULL,
    `user_id` varchar(64) NOT NULL,
    `game_key` varchar(128) NOT NULL,
    `name` varchar(255) NOT NULL,
    `provider` varchar(32) NOT NULL DEFAULT 'CUSTOM',
    `provider_game_id` varchar(128) DEFAULT NULL,
    `cover_file_id` varchar(64) DEFAULT NULL,
    `retention_enabled` tinyint(4) NOT NULL DEFAULT 0,
    `retention_count` int(11) NOT NULL DEFAULT 50,
    `retention_days` int(11) NOT NULL DEFAULT 0,
    `status` tinyint(4) NOT NULL DEFAULT 1,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_library_game_id` (`game_id`),
    UNIQUE KEY `uk_game_library_user_key` (`user_id`, `game_key`),
    UNIQUE KEY `uk_game_library_user_name` (`user_id`, `name`),
    KEY `idx_game_library_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `game_object` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `object_id` varchar(64) NOT NULL,
    `user_id` varchar(64) NOT NULL,
    `sha256` varchar(64) NOT NULL,
    `size` bigint(20) NOT NULL,
    `file_id` varchar(64) NOT NULL,
    `reference_count` bigint(20) NOT NULL DEFAULT 0,
    `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_object_object_id` (`object_id`),
    UNIQUE KEY `uk_game_object_user_hash_size` (`user_id`, `sha256`, `size`),
    UNIQUE KEY `uk_game_object_file_id` (`file_id`),
    KEY `idx_game_object_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `game_snapshot` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `snapshot_id` varchar(64) NOT NULL,
    `user_id` varchar(64) NOT NULL,
    `game_id` varchar(64) NOT NULL,
    `device_id` varchar(64) NOT NULL,
    `parent_snapshot_id` varchar(64) DEFAULT NULL,
    `trigger_type` varchar(32) NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `game_snapshot_file` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `game_sync_head` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id` varchar(64) NOT NULL,
    `game_id` varchar(64) NOT NULL,
    `head_snapshot_id` varchar(64) DEFAULT NULL,
    `version` bigint(20) NOT NULL DEFAULT 0,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_sync_head_user_game` (`user_id`, `game_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
