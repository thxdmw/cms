# cms-kernel 内核模块

纯公共类型、异常、工具、基础契约。**不依赖任何业务模块，也不依赖任何 `com.thx.module.*` 包**。

## 包含的包（都在 `src/main/java/com/thx` 下）

| 包 | 内容 | 约束 |
| --- | --- | --- |
| `com.thx.common.annotation` | `@AnonymousAccess` 等纯注解 | 不得依赖 Spring Web 之外的框架 |
| `com.thx.common.holder` | `SpringContextHolder` | 仅依赖 spring-context |
| `com.thx.common.util` | `JsonUtil` / `UUIDUtil` / `DateUtil` / `Pagination` / `ResultUtil` / `MD5` 等 | 只依赖 JDK / hutool / jackson / mybatis-plus-extension |
| `com.thx.common.vo` | `ResponseVo` / `PageResultVo` / `BaseVo` / `BaseConditionVo`（从原 `module.admin.vo.base` 迁入） | 全局统一响应体，所有模块共用 |
| `com.thx.enums` | `ResponseStatus` / `SysConfigKey` | — |
| `com.thx.exception` | `ApiException` | 纯异常类型（`ExceptionHandleController` 在 cms-platform） |

## 依赖（只声明实际需要的）

- `jackson-databind`（JsonUtil）、`spring-context`（SpringContextHolder/CopyUtil）、`jakarta.servlet-api`（IpUtil）、
  `slf4j-api`（@Slf4j）、`mybatis-plus-extension`（Pagination 继承 Page）、`hutool-core`、lombok

## 约束

- **禁止**在这里放任何依赖业务实体/Mapper/Service 的类——那些属于 cms-platform。
- **禁止**引入重依赖（MinIO/Tika/支付宝 SDK/Sa-Token/Web starter 等），保持内核轻量。
- `Pagination`、`ResultUtil` 等被 file/payment/gamesave 直接使用，改动前先确认所有下游模块。

## 测试

本模块暂无测试类；若新增，必须是纯 JUnit（不启动 Spring 上下文）。
