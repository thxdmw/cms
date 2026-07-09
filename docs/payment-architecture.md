# 支付模块（Payment）架构设计

> 状态：已实现第一版（支付宝 APP 支付完整链路）。本文档是唯一的权威设计说明，代码结构变化时需要同步更新。

## 一、现有项目分析结论

| 检查项 | 结论 |
|---|---|
| Java 版本 | 1.8（本地 JDK 为 1.8.0_401），所有新代码必须 Java 8 兼容（禁止 `var`、Record、Text Block、switch 表达式） |
| Spring Boot 版本 | 2.7.18 |
| 构建工具 | Maven，**单模块**（`packaging=jar`，无 `<modules>`） |
| 当前模块划分 | 包级划分：`com.thx.module.{admin,agent,blog,file,tools}`，无 Maven 子模块边界 |
| 数据库 | **MySQL**（不是 PostgreSQL）。生产环境固定 5.7（见 `docs/db/服务器配置.md`），本地开发机为 8.0.34。因此所有 SQL 必须按 **MySQL 5.7 兼容**编写（不能用 `SELECT ... SKIP LOCKED`、窗口函数、CTE 等 8.0 专属语法） |
| ORM | MyBatis-Plus 3.5.5，Mapper XML 放在 `resources/mapper/*.xml`，也允许纯注解 Mapper（`file` 模块已是纯注解风格），全局 `id-type: auto` |
| Schema 管理 | **没有 Flyway/Liquibase**，通过 `docs/db/*.sql` 手工安装脚本管理（`cms.sql`、`file_system.sql`），本次新增 `docs/db/payment.sql` 遵循同一约定，**不引入 Flyway** |
| 鉴权 | **Apache Shiro**（不是 Spring Security，也不是 Sa-Token），Session 存储可切换 Redis/内存；免登录路径通过 `@AnonymousAccess` 注解 + `AnonymousPathScanner` 动态扫描注册，无需手工维护 URL 白名单 |
| Redis | `spring-data-redis` 的 `RedisTemplate<String,Object>`（Jackson 序列化），**没有 Redisson**，Jedis 2.9.1（因 shiro-redis 兼容性锁定版本，不可升级） |
| 统一返回体 | `com.thx.module.admin.vo.base.ResponseVo<T>`（`status/msg/data`），配套 `ResultUtil` 静态工厂；`ResponseStatus` 枚举提供常用状态码，但 `ResponseVo.error(int status, String msg)` 支持任意自定义 HTTP 语义状态码 |
| 全局异常处理 | `ExceptionHandleController`（`@ControllerAdvice`）处理 `ApiException`/Shiro 异常等全局兜底；但项目已确立"**独立模块可以有自己的 `@RestControllerAdvice`**"先例——`file` 模块的 `FileExceptionHandler` 用 `basePackages` 限定只处理自己模块的异常，返回精确 HTTP 状态码。Payment 模块复用这一先例 |
| ID 生成 | 无 Snowflake/雪花算法组件，只有 `UUIDUtil`（UUID/短 UUID）。业务单号历史上依赖 `DateUtil` 的时间戳格式常量拼接 |
| 时间类型 | 全项目统一使用 `java.util.Date` + MySQL `datetime`（不是 `Instant`/`TIMESTAMPTZ`，项目里没有 PostgreSQL，也没有 JSR-310 落库先例）。Payment 模块沿用 `Date`/`datetime`，字段命名沿用项目既有的 `create_time`/`update_time`（不是 `created_at`/`updated_at`） |
| HTTP Client | 项目内没有沉淀统一 HTTP Client 封装（仅个别模块直接用 `RestTemplate`）。Payment 对接支付宝使用**官方 SDK 内置的 HTTP 调用**，不需要额外选型 |
| 配置管理 | Spring Boot `@ConfigurationProperties`，前缀统一用 `cms.xxx`（如 `cms.file-system`、`cms.http-logging`），敏感值通过环境变量注入（`${XXX:default}`），`.env.example` 集中记录。Payment 沿用 `cms.payment` 前缀 |
| 模块间依赖 | `admin/agent/blog/file/tools` 各自独立，`file` 模块是当前唯一的"多租户基础设施模块"范式（`FileApp` + API Key + Scope，供 CMS 自身和未来 Pet/Game 等应用共用），是 Payment 模块结构上最值得参考的先例 |

**结论：优先复用 MyBatis-Plus / MySQL / Shiro（`@AnonymousAccess`）/ 现有 `ResponseVo` / Jackson / Hutool；不引入 JPA、PostgreSQL、Sa-Token、Redisson、Flyway。唯一新增依赖是 `com.alipay.sdk:alipay-sdk-java`（官方支付宝 SDK，不可替代）。**

## 二、模块定位与放置位置

项目是单 Maven 模块，按用户指令在包结构内新增 `com.thx.module.payment`，与 `admin/agent/blog/file/tools` 平级。选择挂在 `com.thx.module` 下而不是新建顶层命名空间，是因为它在依赖治理上与 `file` 模块同属"面向多业务方的基础设施模块"，与既有目录约定保持一致，`MybatisPlusConfig` 的 `@MapperScan` 也只需新增一行。

Payment 不是"支付宝工具类"，而是承担以下职责的独立基础设施：支付订单 / 支付渠道 / 支付请求 / 支付状态 / 异步通知 / 主动查询 / 关闭 / 退款 / 支付事件 / 幂等 / 审计 / 状态补偿 / Webhook 扩展点。**不**负责会员、积分、商品、库存、优惠券、业务权益发放——这些由业务模块消费 `PaymentEvent` 后自行完成。

### 包结构

```
com.thx.module.payment
├── api/                     # 对业务模块暴露的稳定契约（未来 HTTP/RPC 化时原样搬迁）
│   ├── PaymentFacade.java          # 业务模块唯一允许调用的入口接口
│   ├── command/                    # CreatePaymentCommand / QueryPaymentCommand / CreateRefundCommand / QueryRefundCommand
│   ├── result/                     # CreatePaymentResult / PaymentResult / RefundResult
│   ├── enums/                      # PaymentChannel / PaymentScene（调用方必须传入的枚举）
│   └── event/                      # PaymentSucceededEvent / PaymentClosedEvent / RefundSucceededEvent / @PaymentEventHandler
├── domain/                  # 领域模型：状态枚举 + MyBatis-Plus 实体 + 状态机（零对外部业务模块的依赖）
├── channel/
│   ├── spi/                 # PaymentChannelProvider SPI、Router、Channel*Command/Result、ChannelResultStatus
│   └── alipay/               # 支付宝 Adapter（AlipayClientFactory / AlipayNotifyParser / AlipayPaymentProvider）
├── infrastructure/          # AesGcmCipher（渠道配置加密）、SensitiveDataMasker、PaymentNoGenerator
├── repository/
│   ├── mapper/               # MyBatis-Plus BaseMapper
│   └── *Repository.java      # Application 层唯一可访问的持久化入口（封装 CAS / FOR UPDATE）
├── application/              # PaymentApplicationService / RefundApplicationService / PaymentNotifyService /
│                              # PaymentSyncService / PaymentChannelResultProcessor / PaymentEventService /
│                              # PaymentEventDispatcher / LocalPaymentFacade
├── controller/               # PaymentController / RefundController / AlipayNotifyController
├── scheduler/                # PaymentReconcileScheduler / PaymentEventScheduler
├── config/                   # PaymentProperties
└── exception/                 # PaymentException / PaymentErrorCode / PaymentExceptionHandler
```

> 与用户原始建议的目录（含 `support`、`security`）相比做了两处收敛：`support` 的内容分别落进 `application`（事件分发器/Facade 实现）与 `infrastructure`（工具类），避免出现语义模糊的杂项目录；`security` 未单独建目录，因为 Payment 复用项目已有的 `@AnonymousAccess` 免登录机制，没有引入新的鉴权体系，单独建目录没有实际内容。这是为了贴合本项目现有的扁平化模块约定（对照 `file` 模块），而不是生搬硬套。

### 依赖方向（强约束）

```
业务模块（未来的 pet / agent / saas ...）
        │  只能依赖
        ▼
com.thx.module.payment.api（PaymentFacade + Command/Result + Event)
        │
        ▼
com.thx.module.payment.application（编排）
        │
        ▼
domain / channel / repository / infrastructure
        │
        ▼
common / infra（项目公共基础设施）
```

- 业务模块**禁止**直接引用 `PaymentOrderMapper`、`*Repository`、`AlipayPaymentProvider`，只能注入 `PaymentFacade`。
- `payment` 包内部**不允许**出现任何对业务模块（未来的 `pet`/`user`/`ai` 等）的 import，当前代码库里没有这些模块，天然满足；新增代码评审时要守住这条线。
- `PaymentEvent` 消费采用**监听者反向依赖**：业务模块的 Listener 依赖 `payment.api.event`，`payment` 内核不知道 Listener 的存在（通过反射扫描 `@PaymentEventHandler` 实现），实现"payment 不能反向依赖 pet/user/ai"。

## 三、未来拆分为独立 Payment Center 的路径

当前是"进程内 Payment Center"：业务模块与 Payment 运行在同一 JVM，`LocalPaymentFacade` 直接调用 `PaymentApplicationService`。

```
现在：PetService --(Java 方法调用)--> LocalPaymentFacade --> PaymentApplicationService --> ... --> MySQL(cms 库)
将来：PetService --(HTTP/RPC)-->    HttpPaymentFacade  --> Payment Center 独立服务      --> ... --> MySQL(独立 payment 库)
```

真正拆分时需要做的事情（且**不需要重新设计支付核心业务**）：

1. **搬代码**：把 `com.thx.module.payment` 整包移到新服务，`api` 包下的 DTO 原样作为新服务对外的请求/响应体（已经是与 HTTP 无关的纯 POJO + Jackson 可序列化）。
2. **搬表**：9 张 `payment_*` 表连同数据一起迁到独立的 `payment` 库；项目里没有任何跨库外键（本设计从一开始就不建 DB 级外键，`app_code`/`channel_account_id` 全部是软引用），迁移无需处理外键约束。
3. **换实现类，不换接口**：新建 `HttpPaymentFacade implements PaymentFacade`，内部用 `RestTemplate`/`WebClient` 调 Payment Center 暴露的 `/api/payment/v1/**`（本次已实现，见下），业务模块的 `@Autowired PaymentFacade` 注入点不用改一行代码，只需把 Spring 容器里的 Bean 换成 `HttpPaymentFacade`。
4. **事件投递方式升级**：`PaymentEventDispatcher` 的"进程内反射调用 `@PaymentEventHandler`"替换/新增为"HTTP Webhook 投递"（`PaymentBusinessApp.webhookUrl`/`webhookSecretEncrypted` 字段现在就已经预留），`payload` JSON 结构保持不变，业务侧只需要把方法体搬到一个新的 Webhook Controller 里。
5. 需要新增（当前明确不做，写入"未来 TODO"）：服务间身份认证（API Key/mTLS）、跨服务分布式事务的最终一致性再确认（当前同 JVM 内是本地事务，拆分后 Payment 成功与业务模块消费是两个进程，`PaymentEvent` outbox 机制已经是为这一步预留的）。

## 四、业务来源隔离：PaymentBusinessApp

不复用登录 `userId`，也不把 `appCode` 理解成支付宝 `appId`。`PaymentBusinessApp` 是"谁在使用 Payment 基础设施"的身份，例如 `PET_APP`、`AGENT_PLATFORM`。`CreatePaymentCommand` 必须显式传入 `appCode`，唯一约束落在 `app_code + business_order_no`。字段：`id / app_code / app_name / enabled / webhook_url / webhook_secret_encrypted / create_time / update_time`（`webhook_*` 是为拆分后 Webhook 投递预留，当前不使用但已建列，避免以后加列迁移）。

## 五、支付核心模型

### PaymentOrder（支付订单，业务支付意图，一个）

`id / payment_no / app_code / business_order_no / subject / description / amount(DECIMAL 18,2) / currency / channel / scene / status / expire_time / success_time / close_time / refunded_amount / metadata(JSON) / version / create_time / update_time`。唯一约束 `app_code+business_order_no`、`payment_no`。金额强制 `BigDecimal` ↔ `DECIMAL(18,2)`，禁止 `double/float`。

### PaymentAttempt（一次具体渠道调用尝试，可多个）

`id / attempt_no / payment_no / channel / scene / channel_account_id / channel_trade_no / status / channel_request(JSON,已脱敏) / channel_response(JSON,已脱敏) / failure_code / failure_message / create_time / update_time`。

**关键设计**：调用支付宝时提交的 `out_trade_no = attemptNo`（不是 `paymentNo`）。原因：同一个 `PaymentOrder` 可能因为超时/重试产生第二个 `PaymentAttempt`，如果都用 `paymentNo` 报给支付宝，第二次请求会被支付宝按"商户订单号已存在"拒绝；用 `attemptNo` 保证每次调用的商户订单号唯一，支付宝异步通知回来的 `out_trade_no` 即 `attemptNo`，据此反查 `PaymentAttempt` 再关联 `PaymentOrder`，这正是十四节通知处理流程里"获取 out_trade_no → 查询 PaymentAttempt → 查询 PaymentOrder"的原因。

### RefundOrder（退款单）

`id / refund_no / payment_no / app_code / business_refund_no / amount / currency / reason / status / channel_refund_no / failure_code / failure_message / version / success_time / create_time / update_time`。唯一约束 `app_code+business_refund_no`、`refund_no`。

## 六、支付状态机

`PaymentStatus`：`CREATED / PROCESSING / UNKNOWN / SUCCESS / FAILED / CLOSED / PARTIALLY_REFUNDED / REFUNDED`。

核心原则：**`FAILED` 只表示"渠道明确的业务失败"（如创建阶段支付宝直接拒绝、或查询确认交易不存在且已过合理时效），绝不能表示"本地调用失败/网络超时/结果未知"**——那些情况一律落 `UNKNOWN`，由 `PaymentReconcileScheduler` 主动查询澄清。这是本设计里最重要的语义约束，所有状态写入代码都必须遵守。

合法转换表（`PaymentStateMachine`，`from == to` 恒定允许，视为幂等确认）：

| From | 允许的 To |
|---|---|
| CREATED | PROCESSING, FAILED, UNKNOWN, CLOSED |
| PROCESSING | UNKNOWN, SUCCESS, FAILED, CLOSED |
| UNKNOWN | SUCCESS, FAILED, CLOSED |
| FAILED | **SUCCESS**（唯一修正路径） |
| CLOSED | **SUCCESS**（关闭指令与用户支付成功竞态时的自愈路径，命中时记 WARN 审计日志） |
| SUCCESS | PARTIALLY_REFUNDED, REFUNDED |
| PARTIALLY_REFUNDED | REFUNDED |
| REFUNDED | （终态，无出边） |

其余任何转换请求一律抛 `IllegalPaymentStateTransitionException`。`FAILED -> SUCCESS` 和 `CLOSED -> SUCCESS` 就是为了满足"支付宝真实成功属于高优先级渠道事实，必须能修正本地误判"的要求。

`PaymentAttemptStatus`：`INIT / PROCESSING / UNKNOWN / SUCCESS / FAILED / CLOSED`，语义与 `PaymentStatus` 对齐，但不做独立状态机建模（`PaymentAttempt` 是"一次调用过程"的记录，其正确性由 `PaymentChannelResultProcessor` 的业务编排保证，重复建一套状态机是不必要的抽象）。

## 七、渠道结果的统一处理：Notify 和 Query 复用同一入口

`PaymentChannelResultProcessor` 是本设计的核心收敛点，Notify 和主动 Query 都调用它，不允许两边各写一套"订单成功"逻辑：

```
支付宝异步通知 --parse+verify--> ChannelNotifyResult ---┐
                                                          ├──> PaymentChannelResultProcessor.apply(...)
支付宝主动查询 --------------> ChannelQueryPaymentResult ┘
```

`apply()` 在**一个事务**内：`SELECT ... FOR UPDATE` 锁定目标 `PaymentOrder` 行 → 若目标状态与当前状态相同直接幂等短路（同时校验 `channelTradeNo` 是否一致，不一致记严重审计日志，不静默覆盖）→ 用状态机校验转换合法性（不合法且当前已是"高阶不可逆状态"则安静忽略，视为迟到/不适用的渠道回执，不抛异常炸穿 Notify 接口）→ 合法则更新 `PaymentOrder`/`PaymentAttempt` → 若目标是 `SUCCESS`/`CLOSED` 则在同一事务插入 `PaymentEvent`（`UNIQUE(aggregate_type, aggregate_id, event_type)` 兜底去重）与 `PaymentAuditLog` → 提交。

之所以能不依赖 `SELECT ... FOR UPDATE SKIP LOCKED`（MySQL 5.7 没有）：`PaymentReconcileScheduler` 扫出的候选记录**不做抢占声明**，多个实例即使扫到同一条 `UNKNOWN` 记录也只是各自发起一次只读的 `queryPayment`，最终都汇聚到这同一个加锁 + 状态机 + 唯一索引的收敛点，天然去重，不需要在扫描阶段加锁。

## 八、支付渠道 SPI

```java
public interface PaymentChannelProvider {
    PaymentChannel channel();
    boolean supports(PaymentChannel channel, PaymentScene scene);
    ChannelCreatePaymentResult createPayment(ChannelCreatePaymentCommand command);
    ChannelQueryPaymentResult queryPayment(ChannelQueryPaymentCommand command);
    ChannelClosePaymentResult closePayment(ChannelClosePaymentCommand command);
    ChannelRefundResult refund(ChannelRefundCommand command);
    ChannelQueryRefundResult queryRefund(ChannelQueryRefundCommand command);
    ChannelNotifyResult parseAndVerifyNotify(ChannelNotifyCommand command);
}
```

`PaymentChannelRouter` 按 `(channel, scene)` 从 `List<PaymentChannelProvider>` 中挑选 `supports()==true` 的实现；找不到抛 `UnsupportedPaymentChannelException`。`PaymentApplicationService`/`RefundApplicationService` 内部**没有任何** `if (channel == ALIPAY)`/`switch(channel)` 分支，全部通过 Router 解耦。当前只注册 `AlipayPaymentProvider`（仅 `ALIPAY+APP` 返回 `true`），`WECHAT_PAY/STRIPE/PAYPAL` 只是 `PaymentChannel` 枚举值，没有对应 Provider 类——调用会在 Router 层收到清晰的 `UnsupportedPaymentChannelException`，而不是一个返回假数据或抛 `UnsupportedOperationException` 的伪实现类。

## 九、支付宝 Adapter

- SDK：`com.alipay.sdk:alipay-sdk-java:4.40.272.ALL`（通过 Maven Central 官方索引核实的当前最新稳定版，签名/协议/HTTP 调用全部委托给官方 SDK，不自研）。
- `AlipayChannelConfig`：`appId / gatewayUrl / signType / charset / format / mode(PUBLIC_KEY|CERTIFICATE) / privateKey / alipayPublicKey`（证书模式的字段已预留但未接线，见下）。
- `AlipayClientFactory`：按 `channelAccountId` 缓存 `AlipayClient`（`ConcurrentHashMap`），避免每次下单新建 Client；提供 `evict(channelAccountId)` 支持配置更新后失效重建（当前没有 ChannelAccount 管理后台会触发它，先把机制建好，调用点留给后续管理端接入）。
- `AlipayNotifyParser`：用官方 `AlipaySignature.rsaCheckV1` 验签，不自己实现 RSA/签名算法。
- `AlipayPaymentProvider`：`alipay.trade.app.pay`（创建）/`alipay.trade.query`（查询）/`alipay.trade.close`（关闭）/`alipay.trade.refund`（退款）/`alipay.trade.fastpay.refund.query`（退款查询）全部基于真实 SDK 请求/响应对象实现，`AlipayApiException` 按"网络类异常→`UNKNOWN`，SDK 返回明确 `sub_code` 业务错误→`FAILED`"分流处理，绝不把超时当成失败。
- **证书模式（`CERTIFICATE`）诚实地未接线**：`AlipayConfigMode` 枚举已经预留该值，`AlipayClientFactory` 遇到 `CERTIFICATE` 模式会抛出明确的 `PaymentException`（"证书模式当前未启用，请使用 PUBLIC_KEY 模式"），不是伪实现，是清楚标注能力边界，本次只完整实现 `PUBLIC_KEY` 一种验签模式，满足"当前可以完整实现一种，但不能把代码设计死"的要求。

## 十、ChannelAccount 与配置加密

`payment_channel_account` 存储 `account_code / channel / account_name / config_encrypted(TEXT) / enabled`。`config_encrypted` 是 `AlipayChannelConfig` 序列化为 JSON 后，用 `AesGcmCipher`（`AES/GCM/NoPadding`，Java 内置 `javax.crypto`，未引入新依赖）加密的密文；IV 随机生成并附在密文前一起存储（`Base64(IV(12B) + ciphertext+tag)`）。主密钥来自环境变量 `PAYMENT_MASTER_KEY`（Base64 编码的 16/24/32 字节 AES Key），启动时校验长度，**不落库、不进 Git**，校验逻辑与 `ShiroConfig` 里 `remember-me-cipher-key` 的处理方式保持一致的工程习惯。

## 十一、AppChannelBinding

`payment_app_channel_binding`：`app_code / channel / scene / channel_account_id / enabled / priority`。创建支付时按 `(app_code, channel, scene)` 查启用中、`priority` 最小的一条绑定取得 `channel_account_id`，从而支持"多个业务方共用同一个支付宝商户号"或"各业务方各自独立商户号"两种形态，配置全部在 DB，不写死在 `application.yml`。

## 十二、创建支付与幂等策略

同一 `(appCode, businessOrderNo)` 重复创建：

- 金额/币种与已有订单不一致 → `PaymentOrderConflictException`（绝不允许改已有订单金额）。
- 一致 且已有订单状态为：
  - `SUCCESS` → 直接返回既有订单信息（不再返回可支付的 `payData`）。
  - `PROCESSING` → 若最近一次 `PaymentAttempt` 仍在有效期内则复用其 `payData`（支付宝 `orderStr` 在有效期内可重复拉起，不需要重新下单）；否则创建新的 `PaymentAttempt`（同一 `PaymentOrder` 下的第二次尝试）。
  - `UNKNOWN` → 先同步调用 `PaymentSyncService.syncPayment` 澄清，仍无法确认则拒绝并提示"确认中，请稍后重试"。
  - `CLOSED` / `FAILED` → 拒绝并抛出对应异常，要求业务方使用新的 `businessOrderNo` 重新发起（终态订单不做"复活"，避免审计语义混乱）。

## 十三、PaymentEvent Outbox

`PaymentEvent` 与订单状态变更**同事务**写入（`UNIQUE(aggregate_type, aggregate_id, event_type)` 保证同一笔支付只产生一条 `PAYMENT_SUCCEEDED`，无论是 Notify 还是 Query 触发，也无论重复多少次）。投递分两层：

1. **实时触发**：`PaymentEventService` 在写完事件后 `ApplicationEventPublisher.publishEvent(...)`，由 `@TransactionalEventListener(phase = AFTER_COMMIT)` 监听，事务提交后立即尝试投递，保证正常路径下低延迟。
2. **兜底补偿**：`PaymentEventScheduler` 定期扫描 `PENDING`/到期重试的 `FAILED` 事件，用 `UPDATE ... WHERE id=? AND status=?` 做 CAS 声明（避免同一事件被多实例重复投递），失败按退避表重试。

数据库 `payment_event` 表是唯一可靠事实来源，`ApplicationEvent` 只是加速手段，即使进程崩溃，Scheduler 也能从 DB 恢复投递——这是坚持"不能只用 `@EventListener`"要求的落地方式。

业务模块通过 `@PaymentEventHandler(appCode=..., eventType=...)` 注解方法接入（反射扫描注册，类似项目已有的 `AnonymousPathScanner` 扫描 `@AnonymousAccess` 的方式），`payment` 包不知道、也不 import 任何业务模块。拆分为独立 Payment Center 后，`PaymentEventDispatcher` 的投递方式可以从"反射调用本地 Bean"平滑替换为"HTTP 调 `PaymentBusinessApp.webhookUrl`"，事件 `payload` 结构不变。

## 十四、退款并发与超额保护

`RefundApplicationService.refund()`：`SELECT ... FOR UPDATE` 锁定 `PaymentOrder` 行 → 校验 `refundedAmount + thisAmount <= amount` → 调用支付宝退款 → 成功后在**同一把锁**内 `UPDATE payment_order SET refunded_amount=refunded_amount+?, version=version+1, status=? WHERE payment_no=? AND version=?` 原子更新。行锁保证两个并发退款请求被 MySQL 串行化，第二个请求读到的一定是第一个提交后的最新 `refundedAmount`，从根本上杜绝超额，不依赖"先 SELECT 再 Java 判断再 UPDATE"的竞态写法。

## 十五、并发场景清单与对应机制

| 场景 | 机制 |
|---|---|
| 重复点击支付两次 | `app_code+business_order_no` 唯一索引 + 幂等分支处理 |
| 支付宝重复通知 100 次 | `PaymentOrder` 状态机幂等短路 + `payment_event` 唯一约束，`ChannelNotifyRecord.notify_key` 唯一索引作为第一道防线 |
| Notify 与 Query 同时发现成功 | 二者共用 `PaymentChannelResultProcessor`，`FOR UPDATE` 行锁 + 事件唯一约束保证只产生一条 `PAYMENT_SUCCEEDED` |
| 两个线程并发退款 | `FOR UPDATE` 行锁 + `version` 原子更新 |
| Scheduler 多实例重复处理 | `payment_event` 用 `UPDATE...WHERE status=?` CAS 声明；`payment_attempt` 补偿查询无需声明，因为收敛点已加锁去重（无需 `SKIP LOCKED`） |
| 支付宝创建成功但服务在落库前崩溃 | 支持凭 `attemptNo(=out_trade_no)` 主动查询支付宝恢复现场（`PaymentReconcileScheduler` 覆盖 `INIT`/长时间 `PROCESSING` 的 Attempt） |
| 调用支付宝超时 | 落 `UNKNOWN`，绝不直接落 `FAILED` |

## 十六、异常与返回体

`PaymentException` 携带 `PaymentErrorCode`（`code + defaultHttpStatus`），由 `PaymentExceptionHandler`（`@RestControllerAdvice(basePackages="com.thx.module.payment.controller")`，仿照 `file` 模块的 `FileExceptionHandler` 先例）捕获，返回体复用项目现有的 `com.thx.module.admin.vo.base.ResponseVo`（满足"项目已有统一 Result 就复用"的要求），但 HTTP 状态码使用 `ResponseVo.error(int status, String msg)` 承载精确语义（404/409/422 等），不强行套用 `ResponseStatus` 里粗粒度的 5 个值——这两者并不冲突，`ResponseVo` 本身就支持任意整型状态码。

## 十七、当前 REST API 的鉴权定位（诚实说明，不过度设计）

`/api/payment/v1/**`（Payment/Refund Controller）当前**沿用项目默认 Shiro 会话鉴权**，不做特殊处理——因为本阶段唯一真实调用路径是 `PaymentFacade` 的进程内 Java 调用，这批 REST API 只是为未来拆分预留的骨架，尚未设计服务间身份认证（API Key/mTLS）。这是"上生产前必须处理的问题"之一，见文末清单，不在本次范围内假装解决。

`/api/payment/channel-notify/alipay/{channelAccountCode}` 用 `@AnonymousAccess` 标注，走项目现成的匿名路径机制，安全性由支付宝签名验签保障，不依赖 Shiro。

## 十八、数据库表清单

`payment_business_app / payment_channel_account / payment_app_channel_binding / payment_order / payment_attempt / payment_refund_order / payment_channel_notify_record / payment_event / payment_audit_log`，详见 `docs/db/payment.sql`。金额 `DECIMAL(18,2)`，JSON 列用 MySQL `JSON` 类型（5.7+ 支持），时间 `datetime`。

## 十九、当前已实现 / 未实现

**已实现**：支付宝 APP 支付创建、异步通知（验签+幂等+状态处理）、主动查询、关闭、退款、退款查询、状态机、事件 Outbox（进程内实时触发+定时兜底）、状态补偿 Scheduler、REST API、渠道配置加密存储、审计日志、单元与集成测试。

**未实现（明确的能力边界，非遗漏）**：微信/Stripe/Paypal Provider（按设计只应在真正接入时实现）、支付宝证书模式验签、Payment Center 独立部署后的服务间鉴权、Webhook 投递（预留了 `webhookUrl` 字段但拆分前无实际使用场景）、渠道账号管理后台 UI。
