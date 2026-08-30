# cms-platform 平台层模块

内容管理 + 博客 + 运维 Agent + 工具 + 平台安全实现的聚合层，是整个系统的主体。

## 包含的包

| 包 | 内容 |
| --- | --- |
| `com.thx.module.admin` | 后台管理：内容（article）/权限（auth）/站点（site）/文件（file）/系统（system），entity/mapper/service/vo |
| `com.thx.module.blog` | 前台博客 API（复用 admin 数据层，不维护自己的 entity/mapper） |
| `com.thx.module.agent` | 运维 Agent API 网关（`/agent/api/**`，独立 X-API-Key 鉴权） |
| `com.thx.module.tools` | 小工具（PDF 转 Word/OCR 等，部分对接外部 Python 服务） |
| `com.thx.module.platform.observability` | 平台级可观测能力（`LogService`/`LogServiceImpl`，从原 `module.agent.service` 迁入） |
| `com.thx.common.security` | 登录鉴权：LoginAuthenticator/PasswordService/PermsService/SaTokenPermissionImpl/UrlPermissionRuleService/UserContext |
| `com.thx.common.interceptor` | RequestLoggingInterceptor/CommonDataInterceptor/AgentApiAuthInterceptor |
| `com.thx.common.log` | HttpAccessLogFilter/HttpLogSanitizer/CustomizeAppender/LogMessage(Publisher) |
| `com.thx.common.config.properties` | CorsProperties/FileUploadProperties/HttpLoggingProperties/StaticizeProperties |
| `com.thx.infra` | 邮件发送、WebSocket 推送、匿名路径扫描、通用数据 |
| `com.thx.exception` | `ExceptionHandleController` 全局异常处理（依赖 Sa-Token 异常类型，故放 platform 而非 kernel） |
| `com.thx.common.util` | 仅 `PasswordHelper`/`FileUploadUtil`/`HtmlSanitizer` 三个（依赖 admin 实体/jsoup，不能进 kernel） |

## 依赖

- `cms-kernel`、`cms-file`（admin 的文件控制器/服务使用）
- spring-boot-starter-web / websocket / thymeleaf / mail / data-redis
- sa-token（spring-boot3-starter + redis-jackson）、captcha-core、druid（监控统计）、
  mybatis-plus、spring-security-crypto（BCrypt）、jsoup、hutool-core

## 约束

- **禁止**依赖 payment/gamesave；平台代码不要反向依赖 cms-app（app 只负责装配）。
- 通用响应体一律用 `com.thx.common.vo.ResponseVo`（kernel）；`com.thx.module.admin.vo` 只放业务 VO。
- 拦截器、安全类属于平台，不要放到 kernel 或 app。
- 需要 Spring 装配的配置类（WebMvc/MyBatis/Redis/WebSocket）在 cms-app，平台只提供业务组件。
- 博客助手只开放概览、搜索、详情、分类标签和发布五类契约；响应不得直接返回完整业务实体，避免泄露无关字段并减少模型 Token。

博客助手接口：

- `GET /agent/api/blog/overview`
- `GET /agent/api/blog/articles/search`
- `GET /agent/api/blog/articles/{id}`
- `GET /agent/api/blog/taxonomy`
- `POST /agent/api/blog/articles`

## 测试

单元测试（Mockito，无需外部依赖）：`src/test/java/com/thx/common/**` 与 `src/test/java/com/thx/module/agent/**`。
需要完整应用上下文的登录/密码升级集成测试在 `cms-app/src/test`。

## 以后拆分建议

admin/blog/agent/tools 不应按入口类型拆服务；若拆，先重构成 platform/content 领域模块。
