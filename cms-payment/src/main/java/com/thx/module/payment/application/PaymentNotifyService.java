package com.thx.module.payment.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.channel.spi.ChannelNotifyCommand;
import com.thx.module.payment.channel.spi.ChannelNotifyResult;
import com.thx.module.payment.channel.spi.PaymentChannelProvider;
import com.thx.module.payment.channel.spi.PaymentChannelRouter;
import com.thx.module.payment.domain.ChannelAccount;
import com.thx.module.payment.domain.ChannelNotifyRecord;
import com.thx.module.payment.domain.PaymentAttempt;
import com.thx.module.payment.domain.PaymentOrder;
import com.thx.module.payment.repository.ChannelAccountRepository;
import com.thx.module.payment.repository.ChannelNotifyRecordRepository;
import com.thx.module.payment.repository.PaymentAttemptRepository;
import com.thx.module.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * 渠道异步通知统一处理入口，当前只有支付宝会真正调用（{@code AlipayNotifyController}），
 * 但流程本身是渠道无关的：验签 -&gt; 渠道身份校验 -&gt; 定位 Attempt/Order -&gt; 幂等判断 -&gt;
 * 委托 {@link PaymentChannelResultProcessor}。未来接入新渠道的通知只需要新增
 * Controller（解析该渠道特有的回调载荷格式）+ Provider 实现，不需要改动本类。
 * <p>
 * 只有数据库事务成功（{@link PaymentChannelResultProcessor#apply} 提交）才允许返回 true，
 * Controller 据此决定回给渠道 "success" 还是失败文案触发重试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotifyService {

    private static final int MAX_RESULT_LENGTH = 500;

    private final ChannelAccountRepository channelAccountRepository;
    private final PaymentChannelRouter channelRouter;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final ChannelNotifyRecordRepository notifyRecordRepository;
    private final PaymentChannelResultProcessor resultProcessor;
    private final ObjectMapper objectMapper;

    public boolean handleNotify(PaymentChannel channel, String channelAccountCode, Map<String, String> params, String remoteIp) {
        ChannelAccount account = channelAccountRepository.findByAccountCode(channelAccountCode);
        if (account == null || account.getEnabled() == null || account.getEnabled() != 1
                || !channel.name().equals(account.getChannel())) {
            log.warn("异步通知使用了无效或已禁用的渠道账号: channel={}, accountCode={}, ip={}", channel, channelAccountCode, remoteIp);
            return false;
        }

        PaymentChannelProvider provider = channelRouter.routeByChannel(channel);
        ChannelNotifyCommand notifyCommand = ChannelNotifyCommand.builder()
                .channelAccountId(account.getId())
                .params(params)
                .build();

        ChannelNotifyResult notifyResult;
        try {
            notifyResult = provider.parseAndVerifyNotify(notifyCommand);
        } catch (Exception e) {
            log.error("异步通知验签处理异常: channel={}, accountCode={}", channel, channelAccountCode, e);
            return false;
        }
        if (!notifyResult.isSignatureVerified()) {
            log.warn("异步通知验签失败: channel={}, accountCode={}, outTradeNo={}",
                    channel, channelAccountCode, notifyResult.getOutTradeNo());
            return false;
        }
        if (!notifyResult.isChannelIdentityMatched()) {
            log.error("异步通知渠道身份不匹配，疑似伪造: channel={}, accountCode={}, outTradeNo={}, channelAppId={}",
                    channel, channelAccountCode, notifyResult.getOutTradeNo(), notifyResult.getChannelAppId());
            return false;
        }

        String outTradeNo = notifyResult.getOutTradeNo();
        PaymentAttempt attempt = paymentAttemptRepository.findByAttemptNo(outTradeNo);
        if (attempt == null) {
            log.warn("异步通知引用了不存在的 Attempt: outTradeNo={}", outTradeNo);
            return false;
        }
        PaymentOrder order = paymentOrderRepository.findByPaymentNo(attempt.getPaymentNo());
        if (order == null) {
            log.error("异步通知对应的 PaymentOrder 不存在: paymentNo={}", attempt.getPaymentNo());
            return false;
        }

        String notifyKey = buildNotifyKey(channel, account.getId(), notifyResult);
        ChannelNotifyRecord record = notifyRecordRepository.findByNotifyKey(notifyKey);
        if (record != null && "PROCESSED".equals(record.getProcessStatus())) {
            log.info("重复的异步通知（已处理），直接确认成功: notifyKey={}", notifyKey);
            return true;
        }
        if (record == null) {
            record = new ChannelNotifyRecord()
                    .setChannel(channel.name())
                    .setChannelAccountId(account.getId())
                    .setNotifyKey(notifyKey)
                    .setPaymentNo(order.getPaymentNo())
                    .setChannelTradeNo(notifyResult.getChannelTradeNo())
                    .setRawPayload(writeJson(params))
                    .setSignatureVerified(1)
                    .setProcessStatus("RECEIVED")
                    .setReceivedAt(new Date());
            boolean inserted = notifyRecordRepository.tryInsert(record);
            if (!inserted) {
                // 并发竞争：同时到达的重复通知，重新读取一次，若已被对方处理完成直接返回成功，
                // 否则继续往下走——resultProcessor.apply 本身是幂等安全的，重复调用不会产生副作用。
                ChannelNotifyRecord raced = notifyRecordRepository.findByNotifyKey(notifyKey);
                if (raced != null && "PROCESSED".equals(raced.getProcessStatus())) {
                    return true;
                }
                record = raced != null ? raced : record;
            }
        }

        try {
            resultProcessor.apply(attempt, notifyResult.getResultStatus(), notifyResult.getChannelTradeNo(),
                    notifyResult.getTotalAmount(), null, null);
            record.setProcessStatus("PROCESSED");
            record.setProcessedAt(new Date());
            notifyRecordRepository.update(record);
            return true;
        } catch (Exception e) {
            log.error("处理异步通知业务逻辑失败: notifyKey={}", notifyKey, e);
            record.setProcessStatus("REJECTED");
            record.setProcessResult(truncate(e.getMessage()));
            record.setProcessedAt(new Date());
            notifyRecordRepository.update(record);
            return false;
        }
    }

    /**
     * 幂等键使用归一化后的 {@code ChannelResultStatus}（而不是渠道原始字段值，如支付宝 trade_status），
     * 好处是彻底渠道无关；代价是 TRADE_SUCCESS 之后紧跟的 TRADE_FINISHED 会被视为同一 notifyKey 的重复通知
     * 而不单独落一条通知记录——这是可以接受的，因为二者在我们的模型里都只表示"支付成功"，
     * 不需要区分处理，也不会丢失任何需要处理的业务信号。
     */
    private String buildNotifyKey(PaymentChannel channel, Long channelAccountId, ChannelNotifyResult result) {
        return channel.name() + ":" + channelAccountId + ":" + result.getChannelTradeNo() + ":" + result.getResultStatus();
    }

    private String writeJson(Map<String, String> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > MAX_RESULT_LENGTH ? message.substring(0, MAX_RESULT_LENGTH) : message;
    }
}
