# GameSave Module

GameSave 管理游戏元数据、设备、不可变快照、用户级内容对象和同步 HEAD。

依赖方向固定为：

`module.gamesave -> module.file -> ObjectStorageClient -> MinIO`

约束：

- GameSave 不直接注入 `FileAssetMapper`，文件查询通过 file 模块门面完成。
- GameSave 不直接调用 MinIO。
- Windows 客户端不直接调用 `/api/v1/files`，也不持有 File App API Key。
- SHA-256 + size 是第一阶段内容对象身份；按 userId 隔离去重，不做跨用户物理去重。
- Snapshot 不可变；多设备同步通过 `game_sync_head` CAS 推进，冲突返回 409，不自动合并二进制存档。

当前已落地：

- `docs/db/game_save.sql`：七张 GameSave 业务表和 `game-save/save-object` 文件策略。
- `FileCallerContextFactory.forInternalApp(...)`：把 GameSave 用户身份桥接到 OWNER_ONLY 文件权限。
- `FileObjectLookupService`：在 file 模块边界内按 Hash 查询文件资产并执行权限校验。
- `GameObjectService`：支持 missing check、服务端 checksum 二次校验和并发唯一键去重。

下一阶段：设备 Token 鉴权、Snapshot 事务提交、Sync HEAD CAS 和 409 冲突响应。
