ALTER TABLE `game_cleanup_task`
  ADD COLUMN `worker_id` varchar(128) DEFAULT NULL AFTER `last_error`,
  ADD COLUMN `lease_until` datetime DEFAULT NULL AFTER `worker_id`,
  ADD COLUMN `last_heartbeat_time` datetime DEFAULT NULL AFTER `lease_until`,
  ADD KEY `idx_game_cleanup_lease` (`status`, `lease_until`);

ALTER TABLE `game_object`
  DROP INDEX `idx_game_object_cleanup`,
  ADD KEY `idx_game_object_cleanup` (`status`, `reference_count`, `update_time`);
