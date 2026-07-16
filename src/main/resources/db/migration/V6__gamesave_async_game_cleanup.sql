CREATE TABLE IF NOT EXISTS game_cleanup_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  game_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  cursor BIGINT NOT NULL DEFAULT 0,
  retry_count INT NOT NULL DEFAULT 0,
  last_error VARCHAR(1000) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_game_cleanup_task_id (task_id),
  UNIQUE KEY uk_game_cleanup_user_game (user_id, game_id),
  KEY idx_game_cleanup_status_update (status, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

