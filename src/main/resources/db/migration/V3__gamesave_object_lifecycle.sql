ALTER TABLE `game_object`
    ADD KEY `idx_game_object_cleanup` (`status`, `reference_count`, `create_time`);
