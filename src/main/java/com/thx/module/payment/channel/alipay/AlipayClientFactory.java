package com.thx.module.payment.channel.alipay;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thx.module.payment.domain.ChannelAccount;
import com.thx.module.payment.exception.PaymentErrorCode;
import com.thx.module.payment.exception.PaymentException;
import com.thx.module.payment.infrastructure.AesGcmCipher;
import com.thx.module.payment.repository.ChannelAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 channelAccountId 缓存 {@link AlipayClient} 与解密后的 {@link AlipayChannelConfig}，
 * 避免每次支付都重新构建 Client。{@link #evict} 供渠道账号配置更新后失效重建——当前项目
 * 还没有渠道账号管理后台会触发它，机制先建好，调用点留给后续管理端接入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayClientFactory {

    private final ChannelAccountRepository channelAccountRepository;
    private final AesGcmCipher cipher;
    private final ObjectMapper objectMapper;

    private final Map<Long, AlipayChannelConfig> configCache = new ConcurrentHashMap<>();
    private final Map<Long, AlipayClient> clientCache = new ConcurrentHashMap<>();

    public AlipayChannelConfig getConfig(Long channelAccountId) {
        return configCache.computeIfAbsent(channelAccountId, this::loadConfig);
    }

    public AlipayClient getClient(Long channelAccountId) {
        return clientCache.computeIfAbsent(channelAccountId, this::buildClient);
    }

    public void evict(Long channelAccountId) {
        configCache.remove(channelAccountId);
        clientCache.remove(channelAccountId);
        log.info("已失效支付宝渠道账号缓存: channelAccountId={}", channelAccountId);
    }

    private AlipayChannelConfig loadConfig(Long channelAccountId) {
        ChannelAccount account = channelAccountRepository.findById(channelAccountId);
        if (account == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_CHANNEL_ACCOUNT_NOT_FOUND,
                    "渠道账号不存在: id=" + channelAccountId);
        }
        if (account.getEnabled() == null || account.getEnabled() != 1) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_CHANNEL_ACCOUNT_DISABLED,
                    "渠道账号已禁用: id=" + channelAccountId);
        }
        String json = cipher.decrypt(account.getConfigEncrypted());
        try {
            return objectMapper.readValue(json, AlipayChannelConfig.class);
        } catch (IOException e) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_INTERNAL_ERROR,
                    "渠道账号配置解析失败: id=" + channelAccountId, e);
        }
    }

    private AlipayClient buildClient(Long channelAccountId) {
        AlipayChannelConfig config = getConfig(channelAccountId);
        if (config.getMode() != AlipayConfigMode.PUBLIC_KEY) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_INTERNAL_ERROR,
                    "证书模式(CERTIFICATE)当前未启用，请使用 PUBLIC_KEY 模式: channelAccountId=" + channelAccountId);
        }
        return new DefaultAlipayClient(config.getGatewayUrl(), config.getAppId(), config.getPrivateKey(),
                config.getFormat(), config.getCharset(), config.getAlipayPublicKey(), config.getSignType());
    }
}
