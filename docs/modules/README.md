# CMS 模块资料目录

每个业务模块的说明文档与建表脚本都放在对应目录内，约定为**一个模块一份 `README.md` + 一个初始化 SQL**：

| 目录 | 文档 | 初始化 SQL | 说明 |
|---|---|---|---|
| `platform/` | — | `cms.sql` | CMS 平台核心表：用户、角色、权限、文章、评论、站点配置等 |
| `file/` | `README.md` | `schema.sql` | 文件系统模块：应用、策略、命名空间、文件资产与清理任务 |
| `gamesave/` | `README.md` | `schema.sql` | 游戏存档模块：账号、设备、游戏库、内容对象、快照与同步 |
| `payment/` | `README.md` | `schema.sql` | 支付模块：架构设计 + 业务接入示例（含支付宝渠道） |
| `agent/` | `README.md` | — | 运维 Agent 开放接口说明（走独立 X-API-Key 鉴权） |

## 数据库初始化

**全新环境**按下面的顺序执行四个脚本，顺序不能颠倒——`gamesave` 的种子数据依赖 `file` 模块的表：

```bash
mysql --default-character-set=utf8mb4 -u root -p cms < docs/modules/platform/cms.sql
mysql --default-character-set=utf8mb4 -u root -p cms < docs/modules/file/schema.sql
mysql --default-character-set=utf8mb4 -u root -p cms < docs/modules/payment/schema.sql
mysql --default-character-set=utf8mb4 -u root -p cms < docs/modules/gamesave/schema.sql
```

> 必须带 `--default-character-set=utf8mb4`，否则脚本里的中文注释与种子数据会因字符集不匹配报
> `Incorrect string value`。

**已有环境不要执行这些脚本**——它们含 `DROP TABLE IF EXISTS`，会清空业务数据。
已有环境的结构变更一律走 Flyway，规范见 [`cms-app/src/main/resources/db/migration/README.md`](../../cms-app/src/main/resources/db/migration/README.md)。

### 两类脚本的分工

| | 初始化 SQL（本目录） | Flyway 迁移（`db/migration`） |
|---|---|---|
| 用途 | 全新环境一次性建表 | 已有环境的增量结构变更 |
| 执行方式 | 建库时手工执行 | 应用启动时自动执行 |
| 内容 | 始终代表**最新**结构 | 只包含**本次变更** |

改结构时**两边都要改**：写一个 Flyway 脚本的同时，把同样的变更同步进对应模块的初始化 SQL。
历史上正因为只改迁移脚本、忘了同步，导致新老环境建出来的库结构不一致（缺 `token_expire_time`、
`relative_path_hash`、`game_snapshot_root` 等）。`SchemaInitScriptTest` 会对关键结构点做校验。

## 部署

生产部署由 Drone CI 自动触发（`.drone.yml`）：推送到 `master` → 拉起 MySQL/Redis 跑完整构建与测试
→ 通过后 SSH 到服务器执行 `deploy.sh`。

`deploy.sh` 采用「先构建候选镜像、再切换」的方式：构建期间旧版本继续对外服务，切换后轮询
`/actuator/health` 确认新版本真正就绪，**健康检查失败自动回滚到上一个镜像**。镜像按提交号打标签，
默认保留最近 4 个版本。

环境变量模板见项目根目录 [`.env.example`](../../.env.example)，部署时复制为 `../config/.env` 并填入真实值。

## 生产环境约束

- **MySQL 固定 5.7**（本地开发机为 8.0.34）。因此所有 SQL 必须按 5.7 兼容编写，
  不能使用 `SELECT ... SKIP LOCKED`、窗口函数、CTE 等 8.0 专属语法。
- 应用运行在 **JDK 21**，构建与运行镜像均为 `eclipse-temurin:21`。
