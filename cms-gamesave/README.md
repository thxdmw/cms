# cms-gamesave 游戏存档模块

账号/设备/存档对象/快照/配额/清理，面向游戏客户端的独立业务域。
**直接调用 cms-file 的 Java Service**（`FileSystemService` / `FileObjectLookupService` + `FileCallerContextFactory`），
因此目前与 file 强耦合；file 接口稳定后才能独立部署。

## 包含的包

`com.thx.module.gamesave` 下全部：`config`（GameSaveProperties/GameSaveProtocolLimits）、`controller`、
`service`、`mapper`、`model`、`dto`、`vo`（GameSaveResponse）、`context`、`exception`、`filter`、
`interceptor`（设备 Token 认证）、`task`（清理调度）、`util`。

## 依赖

- `cms-kernel`（JsonUtil/UUIDUtil/AnonymousAccess）
- `cms-file`（存档对象的文件上传/下载）
- spring-boot-starter-web、mybatis-plus-spring-boot3-starter、spring-boot-starter-data-redis（登录限流）

## 约束

- 认证自成体系（设备 Token + 登录限流），**不走 Sa-Token**；注册/登录接口用 `@AnonymousAccess` 标记。
- **禁止**依赖 admin/platform 的任何类；访问文件必须通过 file 的 Service 接口 + `FileCallerContextFactory`，
  禁止直接使用 file 的 Mapper/Entity/Controller。
- 配置前缀 `gamesave.*`（见 `GameSaveProperties`）；种子数据依赖 `file_app`/`file_policy` 表（见 `docs/modules/gamesave/schema.sql`）。

## 测试

单元测试（Mockito，无需外部依赖）：`src/test/java/com/thx/module/gamesave/**`，共 16 个测试类。
需要 MySQL/Redis/MinIO 的集成测试（`GameSave*IntegrationTest`，用 `-Dgamesave.integration=true` 开启）在
`cms-app/src/test`，随完整应用上下文运行。
