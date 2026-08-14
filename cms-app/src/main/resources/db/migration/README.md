# 数据库迁移规范

本目录存放 Flyway 迁移脚本，应用启动时自动执行（配置见 `application.yml` 的 `spring.flyway`）。

> 本文件同时用于保证该目录非空，从而能被打包进 jar；请勿删除。

## 当前状态

2026-08 做过一次**基线重置**：历史迁移脚本 V1~V10 的最终结构已全部合并回各模块的初始化 SQL，
本目录随之清空，`flyway_schema_history` 表数据也已清空。因此现在是一个干净的起点。

**新的迁移脚本从 `V1__xxx.sql` 重新开始编号即可。**

## 两类脚本的分工

| 用途 | 位置 | 何时执行 |
|---|---|---|
| **全新环境初始化** | `docs/modules/<模块>/*.sql`（每模块唯一一个） | 建库时手工执行一次 |
| **已有环境结构变更** | 本目录 `V<版本>__<描述>.sql` | 应用启动时 Flyway 自动执行 |

## 必须遵守的规则

1. **改结构时两边都要改**：新增一个 Flyway 脚本的同时，把相同的结构变更同步进对应模块的
   `schema.sql`，使其始终代表"最新结构"。
   历史上正是因为只改迁移脚本、忘了同步 schema.sql，导致新老环境建出来的库结构不一致
   （缺 `token_expire_time`、`relative_path_hash`、`game_snapshot_root` 等）。
   `SchemaInitScriptTest` 会对关键结构点做校验，但它无法覆盖全部字段，仍需自觉同步。

2. **已执行过的脚本不可修改**：Flyway 会校验校验和（`validate-on-migrate: true`），
   改动已执行的脚本会导致启动失败。要修正只能新增一个版本。

3. **脚本必须可重复安全执行**：建表用 `CREATE TABLE IF NOT EXISTS`，
   种子数据用 `INSERT ... ON DUPLICATE KEY UPDATE`。

4. **每模块只保留一个初始化 SQL**：不要在 `docs/modules/` 下新增额外的一次性 SQL 文件，
   一次性数据订正也应写成 Flyway 脚本，以便有执行记录。

## 命名

```
V<版本号>__<小写下划线描述>.sql
```

例如 `V1__payment_add_refund_reason.sql`。版本号连续递增，描述用模块名开头便于检索。
