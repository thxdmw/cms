package com.thx.module.payment.infrastructure;

import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 落库/落日志前对渠道请求响应快照做敏感字段脱敏。
 * {@code PaymentAttempt.channelRequest/channelResponse} 落库前必须经过这里处理。
 */
@UtilityClass
public class SensitiveDataMasker {

    private static final String MASK = "***MASKED***";

    /** 命中即整体替换为 {@link #MASK} 的字段名（已归一化：小写 + 去掉下划线） */
    private static final Set<String> SENSITIVE_KEYS = buildSensitiveKeys();

    private static Set<String> buildSensitiveKeys() {
        Set<String> keys = new HashSet<>();
        keys.add("privatekey");
        keys.add("secret");
        keys.add("password");
        keys.add("appcertcontent");
        keys.add("alipaycertcontent");
        keys.add("alipayrootcertcontent");
        keys.add("sign");
        keys.add("alipaypublickey");
        keys.add("publickey");
        keys.add("webhooksecret");
        keys.add("masterkey");
        keys.add("apikey");
        keys.add("cert");
        return keys;
    }

    /**
     * 递归脱敏一份 Map，返回新 Map，不修改入参。
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> mask(Map<String, ?> source) {
        if (source == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (isSensitiveKey(key)) {
                result.put(key, MASK);
            } else if (value instanceof Map) {
                result.put(key, mask((Map<String, ?>) value));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase().replace("_", "");
        return SENSITIVE_KEYS.contains(normalized);
    }
}
