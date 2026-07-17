-- GameSave 通过同 JVM 内部调用访问文件模块；sentinel hash 不对应任何可用明文 API Key。
INSERT INTO `file_app` (`app_id`, `app_name`, `api_key_hash`, `scopes`, `quota_bytes`, `status`)
VALUES ('game-save', 'GameSave 存档同步', 'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff',
        'UPLOAD,READ,DELETE,LIST,PRESIGN', NULL, 1)
ON DUPLICATE KEY UPDATE
  `app_name` = VALUES(`app_name`),
  `api_key_hash` = VALUES(`api_key_hash`),
  `scopes` = VALUES(`scopes`),
  `status` = VALUES(`status`);

INSERT INTO `file_policy` (`policy_code`, `max_file_size`, `allowed_extensions`, `allowed_mime_types`,
                           `access_level`, `bucket`, `checksum_required`, `status`)
VALUES ('GAME_SAVE_OBJECT', 536870912, NULL, NULL, 'OWNER_ONLY', 'game-save-private', 1, 1)
ON DUPLICATE KEY UPDATE
  `max_file_size` = VALUES(`max_file_size`),
  `allowed_extensions` = VALUES(`allowed_extensions`),
  `allowed_mime_types` = VALUES(`allowed_mime_types`),
  `access_level` = VALUES(`access_level`),
  `bucket` = VALUES(`bucket`),
  `checksum_required` = VALUES(`checksum_required`),
  `status` = VALUES(`status`);

INSERT INTO `file_app_namespace` (`app_id`, `namespace`, `policy_code`, `status`)
VALUES ('game-save', 'save-object', 'GAME_SAVE_OBJECT', 1)
ON DUPLICATE KEY UPDATE
  `policy_code` = VALUES(`policy_code`),
  `status` = VALUES(`status`);
