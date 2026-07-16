ALTER TABLE `game_device`
    ADD COLUMN `token_expire_time` datetime DEFAULT NULL AFTER `token_hash`;

UPDATE `game_device`
SET `token_expire_time` = DATE_ADD(COALESCE(`update_time`, `create_time`, NOW()), INTERVAL 90 DAY)
WHERE `token_expire_time` IS NULL;

ALTER TABLE `game_device`
    MODIFY COLUMN `token_expire_time` datetime NOT NULL,
    DROP INDEX `uk_game_device_device_id`,
    ADD UNIQUE KEY `uk_game_device_user_device` (`user_id`, `device_id`);
