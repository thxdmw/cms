-- 已部署 GameSave V2 数据库的快照保留策略迁移。
-- 默认关闭自动清理，升级不会主动删除任何现有快照。
ALTER TABLE `game_library`
    ADD COLUMN `retention_enabled` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否启用自动保留清理' AFTER `cover_file_id`,
    ADD COLUMN `retention_count` int(11) NOT NULL DEFAULT 50 COMMENT '最多保留快照数，当前 HEAD 始终保留' AFTER `retention_enabled`,
    ADD COLUMN `retention_days` int(11) NOT NULL DEFAULT 0 COMMENT '保留天数，0 表示不按时间清理' AFTER `retention_count`;