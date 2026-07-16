ALTER TABLE `game_snapshot_file`
    ADD COLUMN `relative_path_hash` char(64) DEFAULT NULL AFTER `relative_path`;

UPDATE `game_snapshot_file`
SET `relative_path_hash` = SHA2(`relative_path`, 256)
WHERE `relative_path_hash` IS NULL;

ALTER TABLE `game_snapshot_file`
    MODIFY COLUMN `relative_path_hash` char(64) NOT NULL,
    DROP INDEX `uk_game_snapshot_file_path`,
    ADD UNIQUE KEY `uk_game_snapshot_file_hash` (`snapshot_id`, `relative_path_hash`);
