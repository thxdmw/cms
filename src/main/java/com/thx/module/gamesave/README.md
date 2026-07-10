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
