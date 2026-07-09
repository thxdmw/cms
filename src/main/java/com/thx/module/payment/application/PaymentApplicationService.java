package com.thx.module.payment.application;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thx.module.payment.api.command.CreatePaymentCommand;
import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.enums.PaymentScene;
import com.thx.module.payment.api.result.CreatePaymentResult;
import com.thx.module.payment.channel.spi.ChannelCreatePaymentCommand;
import com.thx.module.payment.channel.spi.ChannelCreatePaymentResult;
import com.thx.module.payment.channel.spi.ChannelResultStatus;
import com.thx.module.payment.channel.spi.PaymentChannelProvider;
import com.thx.module.payment.channel.spi.PaymentChannelRouter;
import com.thx.module.payment.config.PaymentProperties;
import com.thx.module.payment.domain.AppChannelBinding;
import com.thx.module.payment.domain.ChannelAccount;
import com.thx.module.payment.domain.PaymentAttempt;
import com.thx.module.payment.domain.PaymentAttemptStatus;
import com.thx.module.payment.domain.PaymentAuditAction;
import com.thx.module.payment.domain.PaymentBusinessApp;
import com.thx.module.payment.domain.PaymentOrder;
import com.thx.module.payment.domain.PaymentStatus;
import com.thx.module.payment.exception.PaymentErrorCode;
import com.thx.module.payment.exception.PaymentException;
import com.thx.module.payment.exception.PaymentOrderConflictException;
import com.thx.module.payment.infrastructure.PaymentNoGenerator;
import com.thx.module.payment.infrastructure.SensitiveDataMasker;
import com.thx.module.payment.repository.AppChannelBindingRepository;
import com.thx.module.payment.repository.ChannelAccountRepository;
import com.thx.module.payment.repository.PaymentAttemptRepository;
import com.thx.module.payment.repository.PaymentBusinessAppRepository;
import com.thx.module.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 创建支付流程编排。核心步骤见 docs/payment-architecture.md 第十二节。
 * 本类不持有跨越渠道调用的数据库事务——真正的状态落库与事件创建统一交给
 * {@link PaymentChannelResultProcessor}（内部有自己的短事务），避免长时间持有行锁跨越网络调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationService {

    private final PaymentBusinessAppRepository businessAppRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final AppChannelBindingRepository appChannelBindingRepository;
    private final ChannelAccountRepository channelAccountRepository;
    private final PaymentChannelRouter channelRouter;
    private final PaymentChannelResultProcessor resultProcessor;
    private final PaymentSyncService paymentSyncService;
    private final PaymentNoGenerator noGenerator;
    private final PaymentAuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final PaymentProperties paymentProperties;

    public CreatePaymentResult createPayment(CreatePaymentCommand command) {
        validateCommand(command);
        PaymentBusinessApp app = businessAppRepository.findByAppCode(command.getAppCode());
        if (app == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_APP_NOT_FOUND, "业务方不存在: " + command.getAppCode());
        }
        if (app.getEnabled() == null || app.getEnabled() != 1) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_APP_DISABLED, "业务方已禁用: " + command.getAppCode());
        }

        PaymentOrder existing = paymentOrderRepository.findByAppCodeAndBusinessOrderNo(
                command.getAppCode(), command.getBusinessOrderNo());
        if (existing != null) {
            return handleExistingOrder(existing, command);
        }
        return createNewOrder(app, command);
    }

    private void validateCommand(CreatePaymentCommand command) {
        if (StrUtil.isBlank(command.getAppCode())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PARAM_INVALID, "appCode 不能为空");
        }
        if (StrUtil.isBlank(command.getBusinessOrderNo())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PARAM_INVALID, "businessOrderNo 不能为空");
        }
        if (StrUtil.isBlank(command.getSubject())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PARAM_INVALID, "subject 不能为空");
        }
        if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PARAM_INVALID, "amount 必须大于 0");
        }
        if (command.getAmount().scale() > 2) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PARAM_INVALID, "amount 最多支持 2 位小数");
        }
        if (command.getChannel() == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PARAM_INVALID, "channel 不能为空");
        }
        if (command.getScene() == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PARAM_INVALID, "scene 不能为空");
        }
    }

    private String resolveCurrency(CreatePaymentCommand command) {
        String currency = StrUtil.isBlank(command.getCurrency()) ? "CNY" : command.getCurrency().trim().toUpperCase();
        if (!"CNY".equals(currency)) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_CURRENCY_NOT_SUPPORTED, "当前只支持 CNY: " + currency);
        }
        return currency;
    }

    private CreatePaymentResult createNewOrder(PaymentBusinessApp app, CreatePaymentCommand command) {
        String currency = resolveCurrency(command);
        AppChannelBinding binding = resolveBinding(app.getAppCode(), command.getChannel(), command.getScene());

        String paymentNo = noGenerator.nextPaymentNo();
        PaymentOrder order = new PaymentOrder()
                .setPaymentNo(paymentNo)
                .setAppCode(app.getAppCode())
                .setBusinessOrderNo(command.getBusinessOrderNo())
                .setSubject(command.getSubject())
                .setDescription(command.getDescription())
                .setAmount(command.getAmount())
                .setCurrency(currency)
                .setChannel(command.getChannel().name())
                .setScene(command.getScene().name())
                .setStatus(PaymentStatus.CREATED.name())
                .setExpireTime(command.getExpireTime())
                .setRefundedAmount(BigDecimal.ZERO)
                .setMetadata(writeJson(command.getMetadata()))
                .setVersion(0);
        try {
            paymentOrderRepository.insert(order);
        } catch (DuplicateKeyException e) {
            // 极端并发：两个请求同时通过了"不存在"检查，第二个 INSERT 撞唯一索引，退回幂等分支
            log.info("createPayment 并发竞争同一 businessOrderNo，回退到幂等分支: appCode={}, businessOrderNo={}",
                    app.getAppCode(), command.getBusinessOrderNo());
            PaymentOrder raced = paymentOrderRepository.findByAppCodeAndBusinessOrderNo(
                    app.getAppCode(), command.getBusinessOrderNo());
            if (raced == null) {
                throw e;
            }
            return handleExistingOrder(raced, command);
        }
        auditLogService.record(PaymentAuditAction.PAYMENT_CREATED, app.getAppCode(), paymentNo);

        return executeChannelCreate(order, binding, command.getDescription());
    }

    private CreatePaymentResult handleExistingOrder(PaymentOrder existing, CreatePaymentCommand command) {
        String currency = resolveCurrency(command);
        if (existing.getAmount().compareTo(command.getAmount()) != 0 || !existing.getCurrency().equals(currency)) {
            throw new PaymentOrderConflictException("支付订单参数冲突: paymentNo=" + existing.getPaymentNo()
                    + ", 已有金额=" + existing.getAmount() + " " + existing.getCurrency()
                    + ", 请求金额=" + command.getAmount() + " " + currency);
        }

        PaymentStatus status = PaymentStatus.valueOf(existing.getStatus());
        switch (status) {
            case SUCCESS:
                return toResult(existing, null);
            case PROCESSING:
                return handleProcessingOrder(existing, command);
            case UNKNOWN:
                return handleUnknownOrder(existing, command);
            case CLOSED:
                throw new PaymentException(PaymentErrorCode.PAYMENT_ORDER_CLOSED,
                        "支付订单已关闭，请使用新的 businessOrderNo 重新发起: paymentNo=" + existing.getPaymentNo());
            case FAILED:
                throw new PaymentException(PaymentErrorCode.PAYMENT_ORDER_FAILED,
                        "支付订单已失败，请使用新的 businessOrderNo 重新发起: paymentNo=" + existing.getPaymentNo());
            default:
                // CREATED：极窄的并发窗口内被另一个请求看到（本请求的首次结果尚未落库），提示稍后重试
                throw new PaymentException(PaymentErrorCode.PAYMENT_CHANNEL_UNKNOWN_RESULT,
                        "支付订单状态确认中，请稍后重试: paymentNo=" + existing.getPaymentNo());
        }
    }

    private CreatePaymentResult handleProcessingOrder(PaymentOrder order, CreatePaymentCommand command) {
        PaymentAttempt latest = paymentAttemptRepository.findLatestByPaymentNo(order.getPaymentNo());
        if (latest != null && isAttemptReusable(latest, order)) {
            return toResult(order, parseJsonMap(latest.getChannelResponse()));
        }
        AppChannelBinding binding = resolveBinding(order.getAppCode(),
                PaymentChannel.valueOf(order.getChannel()), PaymentScene.valueOf(order.getScene()));
        return executeChannelCreate(order, binding, command.getDescription());
    }

    private CreatePaymentResult handleUnknownOrder(PaymentOrder order, CreatePaymentCommand command) {
        PaymentOrder synced = paymentSyncService.syncPayment(order.getPaymentNo());
        PaymentStatus newStatus = PaymentStatus.valueOf(synced.getStatus());
        if (newStatus == PaymentStatus.SUCCESS) {
            return toResult(synced, null);
        }
        if (newStatus == PaymentStatus.UNKNOWN) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_CHANNEL_UNKNOWN_RESULT,
                    "支付结果确认中，请稍后重试: paymentNo=" + order.getPaymentNo());
        }
        // 主动查询后状态已经变为 FAILED/CLOSED/PROCESSING，交给统一分支处理（不会再次落入 UNKNOWN，不会死循环）
        return handleExistingOrder(synced, command);
    }

    private boolean isAttemptReusable(PaymentAttempt attempt, PaymentOrder order) {
        PaymentAttemptStatus status = PaymentAttemptStatus.valueOf(attempt.getStatus());
        if (status != PaymentAttemptStatus.INIT && status != PaymentAttemptStatus.PROCESSING) {
            return false;
        }
        return order.getExpireTime() == null || !order.getExpireTime().before(new java.util.Date());
    }

    private AppChannelBinding resolveBinding(String appCode, PaymentChannel channel, PaymentScene scene) {
        AppChannelBinding binding = appChannelBindingRepository.findBestBinding(appCode, channel, scene);
        if (binding == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_BINDING_NOT_FOUND,
                    "未配置可用的渠道绑定: appCode=" + appCode + ", channel=" + channel + ", scene=" + scene);
        }
        return binding;
    }

    private CreatePaymentResult executeChannelCreate(PaymentOrder order, AppChannelBinding binding, String description) {
        ChannelAccount account = channelAccountRepository.findById(binding.getChannelAccountId());
        if (account == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_CHANNEL_ACCOUNT_NOT_FOUND,
                    "渠道账号不存在: id=" + binding.getChannelAccountId());
        }
        PaymentChannel channel = PaymentChannel.valueOf(order.getChannel());
        PaymentScene scene = PaymentScene.valueOf(order.getScene());
        PaymentChannelProvider provider = channelRouter.route(channel, scene);

        String attemptNo = noGenerator.nextAttemptNo();
        PaymentAttempt attempt = new PaymentAttempt()
                .setAttemptNo(attemptNo)
                .setPaymentNo(order.getPaymentNo())
                .setChannel(order.getChannel())
                .setScene(order.getScene())
                .setChannelAccountId(binding.getChannelAccountId())
                .setStatus(PaymentAttemptStatus.INIT.name());
        paymentAttemptRepository.insert(attempt);
        auditLogService.record(PaymentAuditAction.PAYMENT_ATTEMPT_CREATED, order.getAppCode(),
                order.getPaymentNo(), attemptNo, null, null, null);

        String notifyUrl = buildNotifyUrl(channel, account.getAccountCode());
        ChannelCreatePaymentCommand channelCommand = ChannelCreatePaymentCommand.builder()
                .channelAccountId(binding.getChannelAccountId())
                .scene(scene)
                .outTradeNo(attemptNo)
                .subject(order.getSubject())
                .description(description)
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .notifyUrl(notifyUrl)
                .expireTime(order.getExpireTime())
                .build();

        ChannelResultStatus resultStatus;
        String channelTradeNo = null;
        String failureCode = null;
        String failureMessage = null;
        Map<String, Object> payData = null;
        try {
            ChannelCreatePaymentResult channelResult = provider.createPayment(channelCommand);
            attempt.setChannelRequest(writeJson(maskRequestSnapshot(channelCommand)));
            attempt.setChannelResponse(writeJson(channelResult.getRawResponse()));
            resultStatus = channelResult.getResultStatus();
            channelTradeNo = channelResult.getChannelTradeNo();
            failureCode = channelResult.getFailureCode();
            failureMessage = channelResult.getFailureMessage();
            payData = channelResult.getPayData();
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用支付渠道发生未预期异常，判定为 UNKNOWN: attemptNo={}", attemptNo, e);
            resultStatus = ChannelResultStatus.UNKNOWN;
            failureCode = "UNEXPECTED_ERROR";
            failureMessage = e.getMessage();
        }

        PaymentOrder updatedOrder = resultProcessor.apply(attempt, resultStatus, channelTradeNo, null,
                failureCode, failureMessage);

        return CreatePaymentResult.builder()
                .paymentNo(updatedOrder.getPaymentNo())
                .status(PaymentStatus.valueOf(updatedOrder.getStatus()))
                .channel(PaymentChannel.valueOf(updatedOrder.getChannel()))
                .scene(PaymentScene.valueOf(updatedOrder.getScene()))
                .payData(payData)
                .build();
    }

    private String buildNotifyUrl(PaymentChannel channel, String accountCode) {
        String base = paymentProperties.getPublicBaseUrl();
        if (StrUtil.isBlank(base)) {
            return null;
        }
        String prefix = StrUtil.removeSuffix(base, "/");
        return prefix + "/api/payment/channel-notify/" + channel.name().toLowerCase() + "/" + accountCode;
    }

    private CreatePaymentResult toResult(PaymentOrder order, Map<String, Object> payData) {
        return CreatePaymentResult.builder()
                .paymentNo(order.getPaymentNo())
                .status(PaymentStatus.valueOf(order.getStatus()))
                .channel(PaymentChannel.valueOf(order.getChannel()))
                .scene(PaymentScene.valueOf(order.getScene()))
                .payData(payData)
                .build();
    }

    private Map<String, Object> maskRequestSnapshot(ChannelCreatePaymentCommand cmd) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("outTradeNo", cmd.getOutTradeNo());
        map.put("subject", cmd.getSubject());
        map.put("amount", cmd.getAmount());
        map.put("currency", cmd.getCurrency());
        map.put("notifyUrl", cmd.getNotifyUrl());
        return SensitiveDataMasker.mask(map);
    }

    private String writeJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_INTERNAL_ERROR, "序列化失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonMap(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            log.warn("解析渠道响应快照失败: {}", json, e);
            return null;
        }
    }
}
