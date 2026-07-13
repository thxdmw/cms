# GameSave 模块

`com.thx.module.gamesave` 负责游戏存档版本和多设备同步语义，不直接操作 MinIO。

## 模块边界

```text
Windows 客户端
    ↓ 设备 Token
GameSave REST API
    ↓
module.gamesave
    ↓ 内部 FileCallerContext
module.file
    ↓
ObjectStorageClient
    ↓
MinIO
```

GameSave 负责：

- 独立用户账号与设备 Token。
- 云端逻辑游戏库。
- 用户级 SHA-256 内容对象去重。
- 不可变快照和完整 Manifest。
- 每个游戏唯一的同步 HEAD。
- 基于数据库条件更新的 HEAD CAS 冲突检测。

文件模块继续负责：

- FilePolicy。
- OWNER_ONLY 权限。
- App 配额。
- 服务端 SHA-256。
- 文件审计。
- 逻辑删除和延迟物理清理。
- MinIO 对象生命周期。

## 安全原则

桌面客户端禁止直接调用 `/api/v1/files`，也禁止持有 `X-File-Api-Key`。

客户端注册或登录后获得设备 Token，后续请求使用：

```http
Authorization: Bearer gs_xxx
```

设备 Token 明文只在签发响应中返回；服务端 `game_device.token_hash` 只保存 SHA-256。

## 快照原则

Snapshot 创建后不可覆盖。新存档永远创建新 Snapshot：

```text
Snapshot 100
    ↓
Snapshot 101
```

提交快照时先校验完整 Manifest 和所有内容对象，插入快照及文件清单、增加对象引用计数，最后使用 `game_sync_head` 条件 UPDATE 推进 HEAD。CAS 失败抛出 `409 SYNC_CONFLICT`，整个 Spring 事务回滚。

## 注释与文档

GameSave 新增 Java 注释和专用文档统一使用中文；协议字段、代码标识符和稳定错误码保留英文。

## 快照读取与安全恢复

客户端恢复某个快照时使用两个 GameSave 接口：

- `GET /api/game-save/v1/games/{gameId}/snapshots/{snapshotId}`：返回当前设备所属用户可读取的完整 Manifest。
- `GET /api/game-save/v1/objects/{objectId}/download-url`：在校验 `userId + objectId + ACTIVE` 后，委托 `module.file` 生成短时下载地址。

第二个接口不会返回 File App API Key，也不会让客户端直接调用 `module.file`。客户端拿到预签名地址后以无 `Authorization` 请求下载对象，避免将 GameSave 的设备 Token 转发到 MinIO 或其他对象存储服务。
## 快照时间线接口

`GET /api/game-save/v1/games/{gameId}/snapshots?limit=100` 返回当前设备所属用户可见的不可变快照摘要，按创建时间倒序排列，`limit` 被服务端限制在 1 到 200。摘要不包含完整 Manifest、对象存储桶或对象键；恢复完整内容仍需通过单个快照 Manifest 接口和对象短时下载地址接口。

当客户端显式选择保留本机冲突版本时，会携带当前云端 HEAD 作为 CAS 前置条件提交新的不可变 Snapshot。原云端版本不会被删除，而是成为新快照的父版本，双方历史均可从时间线恢复。