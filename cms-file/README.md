# cms-file 文件系统模块

独立的文件系统子模块：MinIO 对象存储、Apache Tika 真实类型嗅探、App/Scope 独立认证、REST API。
有自己独立的响应体（`com.thx.module.file.vo.ResponseVo`）、异常处理和拦截器，是**有意保持松耦合的模块边界**。

## 包含的包

`com.thx.module.file` 下全部：`config`（FileSystemProperties/MinioClientConfig）、`controller`、`service`、
`mapper`、`model`、`storage`（MinIO 客户端抽象）、`interceptor`（App API Key 认证）、`annotation`、
`context`（FileCallerContext）、`enums`、`exception`、`task`（过期文件清理）、`util`、`vo`。

## 依赖

- `cms-kernel`（JsonUtil/UUIDUtil/Pagination 等）
- spring-boot-starter-web（FileController 等 REST 接口）
- mybatis-plus-spring-boot3-starter（file_app/file_policy/file_asset 等表）
- `minio`、`tika-core`、`hutool-core`

## 约束

- **禁止**依赖 `com.thx.module.admin.*` / `com.thx.common.security` / `com.thx.infra` —— 本模块认证自成体系（API Key + Scope），不走 Sa-Token。
- 对上层模块（gamesave/admin）只暴露 Java Service 接口（`FileSystemService` / `FileObjectLookupService` 等），
  上层不得直接操作本模块的 Mapper/Entity。
- 配置前缀 `cms.file-system.*`（见 `FileSystemProperties`），种子数据在 `docs/modules/file/schema.sql`。

## 测试

单元测试（Mockito，无需外部依赖）：`src/test/java/com/thx/module/file/**`。
需要真实 MySQL 的集成测试（`FilePolicyServiceIntegrationTest`）在 `cms-app/src/test`，随完整应用上下文运行。

## 以后独立部署的前置条件

稳定 HTTP API 边界 → 独立 file 数据库 → 调用方（gamesave）远程客户端化。见 `docs/modules/file/README.md`。
