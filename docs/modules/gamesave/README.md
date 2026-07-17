# GameSave 模块部署与接口说明

GameSave 是 CMS 内的游戏存档服务模块，负责独立账号、设备认证、云端游戏库、内容对象去重、不可变快照、跨设备同步冲突、容量配额和快照保留。文件二进制与对象生命周期统一复用 `module.file`，客户端不得直接调用通用文件 API 或持有 File App API Key。

## 目录说明

- `schema.sql`：首次部署的完整 GameSave 业务表与 `module.file` 策略初始化脚本。

## 首次部署

1. 部署 MySQL、Redis 和 MinIO，并配置 CMS 的生产环境变量。
2. 依次执行 `docs/modules/platform/cms.sql`、`docs/modules/file/schema.sql`；GameSave 业务表由 Flyway 自动迁移。全新离线初始化也可以执行本目录的 `schema.sql`。
3. 确保 MinIO 中存在私有 bucket `game-save-private`，并使 CMS 服务端能访问其内网 endpoint。
4. 以 HTTPS 公开 CMS；Windows 客户端仅允许 `localhost` 使用 HTTP。
5. 发布 CMS 后注册一个 GameSave 账号，完成一次上传、快照提交和对象下载验证。

> `schema.sql` 用于全新环境，含有建表前清理语句。已有 GameSave 数据的生产环境禁止直接执行它；启动 CMS 时由 `db/migration/V1...V8` 自动执行增量迁移。V7 会幂等写入 `game-save / GAME_SAVE_OBJECT / save-object` 文件策略，V8 会为游戏清理任务增加租约并把孤儿对象索引切换到 `update_time`。

## 认证与设备

- 注册与登录：`POST /api/game-save/v1/auth/register`、`POST /api/game-save/v1/auth/login`，请求体包含 `username`、`password`、`deviceId`、`deviceName`。
- 设备 ID 与名称由 Windows 客户端自动生成：设备 ID 持久化在本地 SQLite，名称使用当前电脑名；用户不需要填写。
- 其余 `/api/game-save/v1/**` 请求使用 `Authorization: Bearer <deviceToken>`。
- GameSave 请求绕过 CMS 后台 Shiro 会话认证，由 `GameDeviceTokenInterceptor` 返回 JSON 格式 401/403；不会重定向到 `/login`。
- 登录限流默认只使用 TCP 对端地址。仅当反向代理地址明确配置到 `gamesave.trusted-proxy-addresses` 时才接受 `X-Forwarded-For`，避免客户端伪造来源 IP。

## 后台清理安全

- 内容对象重新上传或从 `DELETED` 激活时会刷新 `update_time`；零引用孤儿仅按最近活跃时间进入清理。
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
