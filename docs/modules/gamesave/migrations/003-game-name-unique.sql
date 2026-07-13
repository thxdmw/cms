-- 为每个 GameSave 用户增加游戏名称唯一约束。
-- 执行前请先处理同一 user_id 下重复的 name；否则 ALTER TABLE 会拒绝执行。
ALTER TABLE `game_library`
    ADD UNIQUE KEY `uk_game_library_user_name` (`user_id`, `name`);