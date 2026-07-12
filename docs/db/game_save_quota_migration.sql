-- 已部署 GameSave V2 数据库的用户配额迁移。
-- 新环境直接执行 game_save.sql，无需重复执行本文件。
ALTER TABLE `game_account`
    ADD COLUMN `used_bytes` bigint(20) NOT NULL DEFAULT 0 COMMENT '已预占的去重内容对象字节数'
    AFTER `quota_bytes`;

-- 根据当前 ACTIVE 内容对象重建已用容量，修复升级前已有数据。
UPDATE `game_account` account
LEFT JOIN (
    SELECT `user_id`, COALESCE(SUM(`size`), 0) AS `used_bytes`
    FROM `game_object`
    WHERE `status` = 'ACTIVE'
    GROUP BY `user_id`
) object_usage ON object_usage.`user_id` = account.`user_id`
SET account.`used_bytes` = COALESCE(object_usage.`used_bytes`, 0);