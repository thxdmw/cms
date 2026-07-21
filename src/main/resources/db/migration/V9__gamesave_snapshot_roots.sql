CREATE TABLE IF NOT EXISTS `game_snapshot_root` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `snapshot_id` varchar(64) NOT NULL,
    `root_id` varchar(64) NOT NULL,
    `root_type` varchar(16) NOT NULL,
    `path_template` varchar(1024) DEFAULT NULL,
    `source` varchar(32) NOT NULL,
    `confidence` int(11) NOT NULL DEFAULT 0,
    `include_patterns_json` text NOT NULL,
    `exclude_patterns_json` text NOT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_game_snapshot_root` (`snapshot_id`, `root_id`),
    KEY `idx_game_snapshot_root_snapshot` (`snapshot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
