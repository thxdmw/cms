# 业务模块接入 Payment 示例（以 Pet 模块为例）

> 当前项目还没有真实的 Pet 业务模块，本文档给出的是**可直接复制使用**的完整代码示例，
> 供未来新增业务模块接入时参考。核心原则：业务模块只依赖 `com.thx.module.payment.api` 包下的
> `PaymentFacade`、Command/Result、Event、`@PaymentEventHandler`，不依赖 payment 包内的任何其它类。

## 一、业务方接入准备（DBA / 运维一次性操作）

```sql
-- 1. 登记业务方
INSERT INTO payment_business_app (app_code, app_name, enabled)
VALUES ('PET_APP', 'Pet 图片生成', 1);

-- 2. 登记支付宝渠道账号（config_encrypted 必须由应用侧用 AesGcmCipher 加密后写入，
--    不能直接写明文 SQL；下面只展示加密前的 JSON 结构，实际操作通过管理脚本/工具完成）
-- 明文 JSON 示例：
-- {"appId":"2021000000000000","gatewayUrl":"https://openapi.alipay.com/gateway.do",
--  "signType":"RSA2","charset":"UTF-8","format":"json","mode":"PUBLIC_KEY",
--  "privateKey":"商户RSA2私钥","alipayPublicKey":"支付宝公钥"}
INSERT INTO payment_channel_account (account_code, channel, account_name, config_encrypted, enabled)
VALUES ('alipay-main', 'ALIPAY', '主商户号', '<AesGcmCipher 加密后的密文>', 1);

-- 3. 绑定 PET_APP 使用 alipay-main 的 APP 支付
INSERT INTO payment_app_channel_binding (app_code, channel, scene, channel_account_id, enabled, priority)
VALUES ('PET_APP', 'ALIPAY', 'APP', (SELECT id FROM payment_channel_account WHERE account_code = 'alipay-main'), 1, 0);
```

## 二、Pet 业务表（Pet 模块自己维护，不属于 payment 模块）

```sql
-- Pet 模块自己的业务订单表（示例，字段按实际业务调整）
CREATE TABLE `pet_credit_order`
(
    `id`               bigint(20)     NOT NULL AUTO_INCREMENT,
    `order_no`         varchar(64)    NOT NULL COMMENT '即 payment 的 businessOrderNo',
    `user_id`          bigint(20)     NOT NULL,
    `product_id`       varchar(64)    NOT NULL,
    `credit_amount`    int(11)        NOT NULL COMMENT '购买的额度次数',
    `amount`           decimal(18, 2) NOT NULL,
    `status`           varchar(32)    NOT NULL COMMENT 'CREATED/PAID',
    `create_time`      datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Pet 模块自己维护的支付事件消费幂等记录：event_id 唯一，防止重复发放额度。
-- 业务模块自己维护消费幂等记录（而不是复用 payment_event 的状态），
-- 好处是每个业务模块的幂等边界完全独立，互不影响，也不需要对 payment 表加任何写权限。
CREATE TABLE `pet_payment_consume_record`
(
    `id`          bigint(20)  NOT NULL AUTO_INCREMENT,
    `event_id`    varchar(64) NOT NULL,
    `payment_no`  varchar(32) NOT NULL,
    `order_no`    varchar(64) NOT NULL,
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
```

## 三、Pet 创建支付

```java
package com.thx.module.pet.service.impl;

import com.thx.module.payment.api.PaymentFacade;
import com.thx.module.payment.api.command.CreatePaymentCommand;
import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.enums.PaymentScene;
import com.thx.module.payment.api.result.CreatePaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PetCreditOrderServiceImpl {

    // 业务模块只依赖 PaymentFacade，不依赖 PaymentApplicationService/Repository/AlipayPaymentProvider
    private final PaymentFacade paymentFacade;

    public CreatePaymentResult createCreditOrder(Long userId, String productId, int creditAmount, BigDecimal amount) {
        String orderNo = "PET" + System.currentTimeMillis() + userId;

        // 1. 落 Pet 自己的业务订单（CREATED），此处省略具体 Mapper 调用
        // petCreditOrderMapper.insert(...);

        // 2. 调用 PaymentFacade 创建支付
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("productId", productId);
        metadata.put("userId", String.valueOf(userId));
        metadata.put("creditAmount", creditAmount);

        return paymentFacade.createPayment(CreatePaymentCommand.builder()
                .appCode("PET_APP")
                .businessOrderNo(orderNo)
                .subject("图片生成额度" + creditAmount + "次")
                .description("购买" + creditAmount + "次图片生成额度")
                .amount(amount)
                .currency("CNY")
                .channel(PaymentChannel.ALIPAY)
                .scene(PaymentScene.APP)
                .metadata(metadata)
                .build());
        // CreatePaymentResult.payData = {"orderStr": "..."}，原样返回给 Android 客户端
    }
}
```

Android 客户端拿到 `orderStr` 后自行调用支付宝官方 SDK（`PayTask.pay(orderStr)`），Payment 模块不负责这一步。

**Android 返回的"支付成功"只能用来更新 UI（比如展示"支付处理中"），绝对不能据此增加用户额度**——因为客户端返回成功不代表支付宝已经把钱打过来，真正的钱到账事实以下面第五节的异步通知为准。

## 四、完整链路

```
PetCreditOrderServiceImpl.createCreditOrder()
        │
        ▼
PaymentFacade.createPayment()  (LocalPaymentFacade -> PaymentApplicationService)
        │
        ▼
支付宝 App 支付（AlipayPaymentProvider.createPayment，本地签名生成 orderStr）
        │
        ▼
返回 { paymentNo, orderStr } 给 Android
        │
        ▼
Android 调起支付宝 App 完成支付（这一步不属于 Payment 模块职责）
        │
        ▼
支付宝服务器异步回调 POST /api/payment/channel-notify/alipay/alipay-main
        │
        ▼
PaymentNotifyService 验签 + 幂等 + PaymentChannelResultProcessor.apply()
        │
        ▼
payment_order 状态 = SUCCESS （与下面这行同一个数据库事务）
        │
        ▼
写入 payment_event（PAYMENT_SUCCEEDED）
        │
        ▼
事务提交后，PaymentEventDispatcher 实时触发投递（失败由 PaymentEventScheduler 定时兜底重投）
        │
        ▼
反射调用 PetPaymentEventListener.handle(PaymentSucceededEvent)
        │
        ▼
Pet 模块自己的事务：insert pet_payment_consume_record(eventId) + 增加用户额度
```

## 五、Pet 监听支付成功事件（发放额度）

```java
package com.thx.module.pet.listener;

import com.thx.module.payment.api.event.PaymentEventHandler;
import com.thx.module.payment.api.event.PaymentSucceededEvent;
import com.thx.module.payment.domain.PaymentEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pet 模块订阅支付成功事件，发放图片生成额度。
 * 必须根据 eventId 做消费幂等——PaymentEventScheduler 补偿重投时会重复调用本方法，
 * 支付宝重复通知也可能导致 payment 侧多次触发实时投递信号，本方法必须能安全地被多次调用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PetPaymentEventListener {

    private final PetPaymentConsumeRecordMapper consumeRecordMapper; // 对应 pet_payment_consume_record
    private final PetCreditService petCreditService; // 增加用户额度的具体业务逻辑

    @PaymentEventHandler(appCode = "PET_APP", eventType = PaymentEventType.PAYMENT_SUCCEEDED)
    @Transactional
    public void handle(PaymentSucceededEvent event) {
        PetPaymentConsumeRecord record = new PetPaymentConsumeRecord()
                .setEventId(event.getEventId())
                .setPaymentNo(event.getPaymentNo())
                .setOrderNo(event.getBusinessOrderNo());
        try {
            consumeRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // eventId 已消费过，直接返回成功，不重复发放额度
            log.info("支付事件已消费，跳过重复发放: eventId={}", event.getEventId());
            return;
        }

        Long userId = Long.valueOf((String) event.getMetadata().get("userId"));
        Integer creditAmount = (Integer) event.getMetadata().get("creditAmount");
        petCreditService.addCredit(userId, creditAmount);

        // 同事务更新 Pet 自己的业务订单状态为 PAID，此处省略具体 Mapper 调用
        log.info("已发放图片生成额度: userId={}, creditAmount={}, paymentNo={}",
                userId, creditAmount, event.getPaymentNo());
    }
}
```

**关键点**：`insert consume_record` 和 `增加用户额度` 必须在**同一个事务**（`@Transactional`）内，`insert` 撞唯一约束时依赖 `DuplicateKeyException`（Spring 统一异常翻译）判断"已经消费过"，不需要先 `SELECT` 再判断再 `INSERT`——直接插入失败即代表重复，逻辑更简单也没有竞态窗口。

## 六、退款示例

```java
RefundResult refundResult = paymentFacade.refund(CreateRefundCommand.builder()
        .appCode("PET_APP")
        .paymentNo(paymentNo)
        .businessRefundNo("PET_REFUND_" + orderNo)
        .amount(new BigDecimal("6.00"))
        .reason("用户申请退款")
        .build());
```

Pet 模块同样应该订阅 `REFUND_SUCCEEDED` 事件来扣减已发放的额度（如果额度还没使用完）：

```java
@PaymentEventHandler(appCode = "PET_APP", eventType = PaymentEventType.REFUND_SUCCEEDED)
@Transactional
public void handleRefund(RefundSucceededEvent event) {
    // 同样需要基于 event.getEventId() 做消费幂等
}
```

## 七、主动查询（补充手段，不是主路径）

如果 Pet 模块自己的业务场景需要立即知道支付结果（例如用户在等待页面轮询），可以调用：

```java
PaymentResult result = paymentFacade.queryPayment(QueryPaymentCommand.builder()
        .paymentNo(paymentNo)
        .build());
if (result.getStatus() == PaymentStatus.SUCCESS) {
    // 但依然不能在这里直接发放额度！额度发放只能由 PetPaymentEventListener 消费
    // PaymentSucceededEvent 完成，这里只用于前端展示"已支付"提示。
}
```

## 八、不要做的事

- 不要在 Pet 模块里直接注入 `PaymentOrderMapper`/`PaymentOrderRepository`/`AlipayPaymentProvider`。
- 不要根据 Android 客户端的支付结果回调发放额度。
- 不要在 `PetPaymentEventListener` 里跳过 `pet_payment_consume_record` 幂等检查。
- 不要假设 `PaymentSucceededEvent` 只会被投递一次——它可能因为补偿重试被多次调用，Listener 必须幂等。
