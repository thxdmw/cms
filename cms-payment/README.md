# cms-payment 支付模块

支付/退款/异步通知/对账/事件分发。**模块边界最完整的一个**：对外只通过稳定的 `api` 包
（`PaymentFacade` + command/result/enums/event）暴露能力，内部实现（application/channel/domain/repository）不泄漏给其他模块。

## 包含的包

`com.thx.module.payment` 下全部：`api`（对外契约）、`application`（应用服务）、`channel`（渠道 SPI + 支付宝实现）、
`controller`、`config`、`domain`（领域模型）、`exception`、`infrastructure`（AES-GCM/单号生成）、
`repository`（含 `mapper` 子包）、`scheduler`（对账/事件补偿）。

## 依赖

- `cms-kernel`（DateUtil/UUIDUtil/AnonymousAccess 等）
- spring-boot-starter-web（PaymentController/RefundController/AlipayNotifyController）
- mybatis-plus-spring-boot3-starter、`alipay-sdk-java`、`hutool-core`

## 约束

- 依赖方向只有 `payment → kernel`：**禁止**依赖 admin/file/gamesave/platform；接口响应统一用
  `com.thx.common.vo.ResponseVo`（已从 `module.admin.vo.base` 迁走）。
- 其他模块调用支付能力只能通过 `api.PaymentFacade`，禁止直接注入 application/channel/repository 层的类。
- 配置前缀 `cms.payment.*`（见 `PaymentProperties`），主密钥 AES-GCM 加密，禁止把私钥写进代码仓库。

## 测试

单元测试（Mockito，无需外部依赖）：`src/test/java/com/thx/module/payment/**`，共 9 个测试类，覆盖
application/channel/domain/infrastructure 各层。

## 以后独立部署的前置条件

去掉对 `com.thx.common.vo.ResponseVo` 的依赖（改用自带 result 类型）、独立数据库、服务鉴权、Webhook/outbox。
见 `docs/modules/payment/README.md`。
