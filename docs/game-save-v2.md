# GameSave V2 后端接入说明

## 当前范围

当前分支已经打通第一条云端同步闭环：

```text
注册/登录
    ↓
设备 Token
    ↓
创建或读取云端逻辑游戏
    ↓
读取云端 HEAD
    ↓
批量检查缺失内容对象
    ↓
上传缺失对象
    ↓
提交完整 Snapshot Manifest
    ↓
Manifest 未变化？
    ├─ 是：返回当前 HEAD，幂等 no-op
    └─ 否：CAS 推进 game_sync_head
```

## 已实现

- `game_account`：独立 GameSave 用户账号。
- `game_device`：设备身份和设备 Token Hash。
- `game_library`：用户云端逻辑游戏。
- `game_object`：用户级 SHA-256 内容对象。
- `game_snapshot`：不可变快照。
- `game_snapshot_file`：快照完整文件清单。
- `game_sync_head`：每个用户、每个游戏唯一同步 HEAD。
- PBKDF2-HMAC-SHA256 密码哈希。
- 256 bit 随机设备 Token 注册、登录和轮换。
- `game_device.token_hash` 唯一索引，设备认证热路径按 Hash 直接定位。
- Bearer 认证 scheme 大小写不敏感解析。
- 文件系统 `OWNER_ONLY` 内部身份桥接。
- 内容对象缺失检查、上传、服务端 SHA-256 二次校验和并发去重。
- GameObject 上传不包裹 FileSystem 上传长事务；文件上传完成自身事务后再建立 `game_object` 关系。
- `game_object` 落库失败或 checksum 不一致时，逻辑删除本次刚上传的 FileAsset；补偿异常不覆盖原始异常。
- 快照路径规范化、大小写不敏感判重和路径穿越拒绝。
- 新增/修改/删除文件变化数统计。
- Snapshot、Manifest、对象引用计数、HEAD CAS 在同一 Spring 事务内提交。
- 已有 HEAD 且 Manifest 完全一致时返回 `created=false`，不创建重复 Snapshot、不写重复 Manifest、不增加引用计数、不推进 HEAD。

## 文件上传与事务边界

对象存储不参与 MySQL 事务，因此不能把 MinIO 上传简单包在 GameObject 外层数据库事务里。

当前边界：

```text
FileSystemService.upload
    ↓
MinIO put
    ↓
file_asset insert
    ↓
文件模块事务提交

GameObjectService.put
    ↓
game_object insert
    ├─ 成功：完成
    └─ 失败：逻辑删除本次新上传 FileAsset
```

`FileSystemService.upload` 自己负责“MinIO 已写入但 file_asset 落库失败”的对象删除补偿；GameSave 负责“FileAsset 已成功建立但 game_object 关系建立失败”的业务层补偿。两个模块各自处理自己能够感知的失败边界。

## 客户端安全边界

桌面客户端永远不接收 `X-File-Api-Key`，也不直接访问 `/api/v1/files`。

GameSave API 使用：

```http
Authorization: Bearer <device-token>
```

设备 Token 明文只在注册或登录成功响应中返回。服务端只保存 SHA-256 Hash。

## HEAD CAS

客户端提交：

```text
expectedHeadSnapshotId = 本机最后确认的云端 HEAD
```

服务端最终执行条件更新：

```text
UPDATE game_sync_head
SET head_snapshot_id = newHead,
    version = version + 1
WHERE user_id = 当前用户
  AND game_id = 当前游戏
  AND head_snapshot_id = expectedHead
```

更新行数为 0 时返回：

```text
HTTP 409
code = SYNC_CONFLICT
```

该异常触发快照事务回滚，已经插入的 Snapshot、Manifest 和对象引用计数不会残留。

## 零变化同步

当当前 HEAD 已存在，且本次完整 Manifest 与父快照相比 `changedFileCount == 0`：

```text
不 INSERT game_snapshot
不 INSERT game_snapshot_file
不 reference_count + 1
不推进 game_sync_head
```

服务端再次确认 HEAD/Version 未变化后返回：

```text
snapshotId = 当前 HEAD
headVersion = 当前 Version
changedFileCount = 0
created = false
```

客户端据此显示“存档内容没有变化，未创建重复版本”。

## 自动构建与测试

`.github/workflows/gamesave-v2-build.yml` 使用 JDK 8 和 Maven：

1. `mvn -B -DskipTests package` 验证 JDK 8 编译与打包。
2. 执行 GameSave 专用单元测试。
3. 保存 Maven 构建/测试日志 artifact。

当前 GameSave 专用测试覆盖：

- PBKDF2 密码哈希与随机 Salt。
- 设备 Token 格式、随机性和 SHA-256。
- checksum 不一致后的新上传 FileAsset 补偿删除。
- `game_object` 插入失败后的 FileAsset 补偿删除。
- 补偿删除失败不覆盖原始数据库异常。
- 零变化 Manifest 不创建重复 Snapshot、不写 Manifest、不增加引用计数、不推进 HEAD。

仓库原有部分 Spring 上下文/文件策略集成测试依赖真实 MySQL。无数据库 Runner 会在 `sysConfigServiceImpl` 初始化阶段连接失败，因此后续单独增加 MySQL service-container 集成测试，不把外部数据库缺失误判为 GameSave 编译失败。

## 注释与文档规范

`module.gamesave` 新增 Java 注释和 GameSave 专用文档统一使用中文。稳定协议字段、HTTP Header、数据库表名和业务错误码保留英文标识，避免破坏接口契约。

## 合并前验证

除自动构建外，还需要在测试 MySQL/MinIO 环境验证数据库初始化、设备 Token 轮换、并发内容对象上传、双设备 HEAD CAS 冲突，以及客户端到 CMS 的完整同步闭环。

## 尚未实现

- 用户级存储配额原子预占与释放。
- 云端 Snapshot 列表和时间线查询。
- Snapshot Manifest 下载接口。
- 客户端安全恢复事务。
- Snapshot 删除、引用计数递减和零引用对象释放。
- 游戏进程退出自动快照。
- 多设备冲突选择界面。

## 设备管理与快照生命周期

设备管理接口只接受经过设备 Bearer Token 认证的请求：

- `GET /api/game-save/v1/devices` 返回当前账户的设备安全摘要，不返回 Token 或 Token Hash。
- `DELETE /api/game-save/v1/devices/{deviceId}` 撤销其他设备；当前设备受到保护，避免当前会话被误撤销。

历史快照通过 `DELETE /api/game-save/v1/games/{gameId}/snapshots/{snapshotId}` 删除。服务端先校验游戏、用户和快照归属，并拒绝删除当前同步 HEAD。删除事务依次释放每个内容对象引用，再使用带 `snapshot_id + user_id + game_id + ACTIVE` 条件的原子更新标记快照删除；并发状态变化会使整个事务回滚。

内容对象引用降为零时，`game_object` 转为 `DELETED`，对应 FileAsset 进入文件模块既有的逻辑删除、宽限期和失败清理重试流程。仍被其他快照引用的对象不会删除。

本地端到端环境见 `docs/gamesave-e2e.md`，脚本覆盖注册、设备登录、对象上传、两次快照提交、旧 HEAD 冲突、时间线、Manifest 和下载内容 SHA-256 校验。
## 用户存储配额

`game_account.used_bytes` 记录用户当前 ACTIVE 去重内容对象的物理字节数。新对象上传前通过单条条件 UPDATE 原子预占，只有同时满足账户启用、字节数合法且剩余配额充足时才成功；相同用户的相同 `sha256 + size` 对象去重命中不会重复计费。

上传校验、对象关系落库或并发唯一键竞争失败时释放本次预占。删除历史快照导致某个内容对象最后一个引用消失时，服务端先将对象按零引用条件标记删除，再释放对应容量。`GET /api/game-save/v1/account/quota` 返回配额、已用容量和剩余容量，且与其他 GameSave 业务接口一样受设备 Token 认证保护。

新环境由 `docs/db/game_save.sql` 创建 `used_bytes`；已部署环境执行 `docs/db/game_save_quota_migration.sql`，并按现有 ACTIVE 内容对象重建已用容量。
## 快照保留策略

每个 `game_library` 可独立配置是否启用自动清理、最多保留快照数和最长保留天数。升级迁移默认关闭自动清理，因此不会在用户确认前删除任何现有版本。数量范围为 1 到 500；天数范围为 0 到 3650，0 表示不按时间清理。

保留任务始终跳过当前 `game_sync_head.head_snapshot_id`。满足“超出数量”或“早于保留天数”任一条件的非 HEAD 快照会复用正常快照删除事务，递减对象引用、释放零引用文件和用户配额。后台任务启动五分钟后首次执行，之后每六小时执行；单个游戏每轮最多读取 2000 个快照，避免异常历史导致内存无界增长。用户也可调用 `POST /api/game-save/v1/games/{gameId}/retention/cleanup` 立即执行。