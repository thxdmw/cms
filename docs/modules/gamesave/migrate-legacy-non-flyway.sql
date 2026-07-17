-- 仅用于“已有 GameSave 表、但没有 flyway_schema_history”的旧部署。
-- 必须先完整备份数据库。本脚本把历史 docs schema 幂等补齐到 V8 结构；
-- 成功后还必须使用 Flyway 7.15 执行 baselineVersion=8，再启动 CMS。

SET NAMES utf8mb4;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='game_device' AND column_name='token_expire_time')=0,
  'ALTER TABLE game_device ADD COLUMN token_expire_time datetime DEFAULT NULL AFTER token_hash', 'SELECT 1');
PREPARE gs_stmt FROM @ddl; EXECUTE gs_stmt; DEALLOCATE PREPARE gs_stmt;
UPDATE game_device SET token_expire_time=DATE_ADD(COALESCE(update_time,create_time,NOW()),INTERVAL 90 DAY)
WHERE token_expire_time IS NULL;
ALTER TABLE game_device MODIFY COLUMN token_expire_time datetime NOT NULL;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='game_device' AND index_name='uk_game_device_device_id')>0,
  'ALTER TABLE game_device DROP INDEX uk_game_device_device_id', 'SELECT 1');
PREPARE gs_stmt FROM @ddl; EXECUTE gs_stmt; DEALLOCATE PREPARE gs_stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='game_device' AND index_name='uk_game_device_user_device')=0,
  'ALTER TABLE game_device ADD UNIQUE KEY uk_game_device_user_device (user_id,device_id)', 'SELECT 1');
PREPARE gs_stmt FROM @ddl; EXECUTE gs_stmt; DEALLOCATE PREPARE gs_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='game_snapshot_file' AND column_name='relative_path_hash')=0,
  'ALTER TABLE game_snapshot_file ADD COLUMN relative_path_hash char(64) DEFAULT NULL AFTER relative_path', 'SELECT 1');
PREPARE gs_stmt FROM @ddl; EXECUTE gs_stmt; DEALLOCATE PREPARE gs_stmt;
UPDATE game_snapshot_file SET relative_path_hash=SHA2(relative_path,256) WHERE relative_path_hash IS NULL;
ALTER TABLE game_snapshot_file MODIFY COLUMN relative_path_hash char(64) NOT NULL;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='game_snapshot_file' AND index_name='uk_game_snapshot_file_path')>0,
  'ALTER TABLE game_snapshot_file DROP INDEX uk_game_snapshot_file_path', 'SELECT 1');
PREPARE gs_stmt FROM @ddl; EXECUTE gs_stmt; DEALLOCATE PREPARE gs_stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='game_snapshot_file' AND index_name='uk_game_snapshot_file_hash')=0,
  'ALTER TABLE game_snapshot_file ADD UNIQUE KEY uk_game_snapshot_file_hash (snapshot_id,relative_path_hash)', 'SELECT 1');
PREPARE gs_stmt FROM @ddl; EXECUTE gs_stmt; DEALLOCATE PREPARE gs_stmt;

CREATE TABLE IF NOT EXISTS game_cleanup_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  game_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `cursor` BIGINT NOT NULL DEFAULT 0,
  retry_count INT NOT NULL DEFAULT 0,
  last_error VARCHAR(1000) NULL,
  worker_id VARCHAR(128) NULL,
  lease_until DATETIME NULL,
  last_heartbeat_time DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_game_cleanup_task_id (task_id),
  UNIQUE KEY uk_game_cleanup_user_game (user_id,game_id),
  KEY idx_game_cleanup_status_update (status,update_time),
  KEY idx_game_cleanup_lease (status,lease_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='game_cleanup_task' AND column_name='worker_id')=0,
  'ALTER TABLE game_cleanup_task ADD COLUMN worker_id varchar(128) DEFAULT NULL AFTER last_error', 'SELECT 1');
PREPARE gs_stmt FROM @ddl; EXECUTE gs_stmt; DEALLOCATE PREPARE gs_stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='game_cleanup_task' AND column_name='lease_until')=0,
  'ALTER TABLE game_cleanup_task ADD COLUMN lease_until datetime DEFAULT NULL AFTER worker_id', 'SELECT 1');
PREPARE gs_stmt FROM @ddl; EXECUTE gs_stmt; DEALLOCATE PREPARE gs_stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='game_cleanup_task' AND column_name='last_heartbeat_time')=0,
  'ALTER TABLE game_cleanup_task ADD COLUMN last_heartbeat_time datetime DEFAULT NULL AFTER lease_until', 'SELECT 1');
PREPARE gs_stmt FROM @ddl; EXECUTE gs_stmt; DEALLOCATE PREPARE gs_stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='game_cleanup_task' AND index_name='idx_game_cleanup_lease')=0,
  'ALTER TABLE game_cleanup_task ADD KEY idx_game_cleanup_lease (status,lease_until)', 'SELECT 1');
PREPARE gs_stmt FROM @ddl; EXECUTE gs_stmt; DEALLOCATE PREPARE gs_stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='game_object' AND index_name='idx_game_object_cleanup')>0,
  'ALTER TABLE game_object DROP INDEX idx_game_object_cleanup', 'SELECT 1');
PREPARE gs_stmt FROM @ddl; EXECUTE gs_stmt; DEALLOCATE PREPARE gs_stmt;
ALTER TABLE game_object ADD KEY idx_game_object_cleanup (status,reference_count,update_time);

INSERT INTO file_app (app_id,app_name,api_key_hash,scopes,quota_bytes,status)
VALUES ('game-save','GameSave 存档同步','ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff',
        'UPLOAD,READ,DELETE,LIST,PRESIGN',NULL,1)
ON DUPLICATE KEY UPDATE app_name=VALUES(app_name),api_key_hash=VALUES(api_key_hash),
                        scopes=VALUES(scopes),status=VALUES(status);
INSERT INTO file_policy (policy_code,max_file_size,allowed_extensions,allowed_mime_types,
                         access_level,bucket,checksum_required,status)
VALUES ('GAME_SAVE_OBJECT',536870912,NULL,NULL,'OWNER_ONLY','game-save-private',1,1)
ON DUPLICATE KEY UPDATE max_file_size=VALUES(max_file_size),access_level=VALUES(access_level),
                        bucket=VALUES(bucket),checksum_required=VALUES(checksum_required),status=VALUES(status);
INSERT INTO file_app_namespace (app_id,namespace,policy_code,status)
VALUES ('game-save','save-object','GAME_SAVE_OBJECT',1)
ON DUPLICATE KEY UPDATE policy_code=VALUES(policy_code),status=VALUES(status);
