# GameSave 模块部署与接口说明

GameSave 是 CMS 内的游戏存档服务模块，负责独立账号、设备认证、云端游戏库、内容对象去重、不可变快照、跨设备同步冲突、容量配额和快照保留。文件二进制与对象生命周期统一复用 `module.file`，客户端不得直接调用通用文件 API 或持有 File App API Key。

## 目录说明

- `schema.sql`：首次部署的完整 GameSave 业务表与 `module.file` 策略初始化脚本。

## 首次部署

1. 部署 MySQL、Redis 和 MinIO，并配置 CMS 的生产环境变量。
2. 按顺序执行各模块初始化 SQL：`docs/modules/platform/cms.sql` → `docs/modules/file/schema.sql` → `docs/modules/payment/schema.sql` → 本目录的 `schema.sql`（GameSave 的种子数据依赖 file 模块的表，顺序不能颠倒）。
3. 确保 MinIO 中存在私有 bucket `game-save-private`，并使 CMS 服务端能访问其内网 endpoint。
4. 以 HTTPS 公开 CMS；Windows 客户端仅允许 `localhost` 使用 HTTP。
5. 发布 CMS 后注册一个 GameSave 账号，完成一次上传、快照提交和对象下载验证。

> `schema.sql` 是本模块结构的唯一事实来源，始终代表最新结构，并含有建表前的清理语句（`DROP TABLE IF EXISTS`）。
> **已有数据的环境禁止执行它**——那会清空业务数据；已有环境的结构变更一律走 Flyway，见 `src/main/resources/db/migration/README.md`。

## 认证与设备

- 注册与登录：`POST /api/game-save/v1/auth/register`、`POST /api/game-save/v1/auth/login`，请求体包含 `username`、`password`、`deviceId`、`deviceName`。
- 设备 ID 与名称由 Windows 客户端自动生成：设备 ID 持久化在本地 SQLite，名称使用当前电脑名；用户不需要填写。
- 其余 `/api/game-save/v1/**` 请求使用 `Authorization: Bearer <deviceToken>`。
- GameSave 请求绕过 CMS 后台 Sa-Token 会话认证，由 `GameDeviceTokenInterceptor` 返回 JSON 格式 401/403；不会重定向到 `/login`。
- 登录限流默认只使用 TCP 对端地址。仅当反向代理地址明确配置到 `gamesave.trusted-proxy-addresses` 时才接受 `X-Forwarded-For`，避免客户端伪造来源 IP。

## 后台清理安全

- `check-missing` 或重复上传命中 `ACTIVE` 对象时会刷新 `update_time`，从 `DELETED` 激活时同样刷新；零引用孤儿的最终状态抢占会再次原子校验活跃时间阈值，旧查询结果不能覆盖刚被复用的对象。
- 游戏删除任务使用 `worker_id + lease_until + last_heartbeat_time` 租约。CMS 在批次中崩溃后，租约到期的 `RUNNING` 任务可被其他实例重新认领。
- 快照、游标、游戏和任务的关键状态更新都检查影响行数；幂等完成仅在数据库已处于目标状态时接受。

## 主要接口

| 功能 | 接口 |
| --- | --- |
| 注册、登录 | `POST /auth/register`、`POST /auth/login` |
| 游戏库 | `GET/POST /games`、`DELETE /games/{gameId}` |
| 缺失对象检查、上传、下载地址 | `POST /objects/check`、`POST /objects`、`GET /objects/{objectId}/download-url` |
| 提交、读取和删除快照 | `POST /games/{gameId}/snapshots`、`GET /games/{gameId}/snapshots`、`GET /games/{gameId}/snapshots/{snapshotId}`、`DELETE /games/{gameId}/snapshots/{snapshotId}` |
| 同步 HEAD | `GET /games/{gameId}/head` |
| 配额 | `GET /account/quota` |
| 设备管理 | `GET /devices`、`DELETE /devices/{deviceId}` |
| 快照保留 | `GET/PUT /games/{gameId}/retention`、`POST /games/{gameId}/retention/cleanup` |

## 自动化验证

- 普通 `mvn test` 执行单元、契约和 Spring 上下文测试。
- `.github/workflows/gamesave-integration.yml` 在 `main/master/dev` 推送、PR 或手动触发时启动 MySQL 5.7、Redis 和 MinIO。
- 目标环境测试覆盖注册、登录、Redis 限流键、缺失对象检查、上传、真正并发的双设备快照提交、HEAD、预签名下载、快照删除、对象清理、配额恢复和游戏后台清理。
- 真实 MySQL 行锁测试覆盖对象触碰/孤儿抢占、快照引用/删除抢占、清理完成/重新激活，以及过期租约的双 Worker 认领。
- `SchemaInitScriptTest` 校验 `schema.sql` 是否包含全部已合并的历史结构（设备 Token 过期时间、路径哈希唯一键、清理租约字段、快照根表、文件策略种子等），防止初始化脚本与实际结构再次漂移。
