package com.thx.module.gamesave.service.impl;

import com.thx.module.gamesave.config.GameSaveProperties;
import com.thx.module.gamesave.exception.GameSaveException;
import com.thx.module.gamesave.service.GameAuthRateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** 基于 Redis 固定窗口计数的认证限流。Redis 故障时记录告警并保持服务可用。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameAuthRateLimitServiceImpl implements GameAuthRateLimitService {

    private static final String PREFIX = "gamesave:auth:";

    private final StringRedisTemplate redisTemplate;
    private final GameSaveProperties properties;

    @Override
    public void assertLoginAllowed(String username, String ip) {
        String normalizedUsername = normalize(username);
        assertBelow(userIpKey(normalizedUsername, ip), properties.getLoginUserIpFailures());
        assertBelow(ipKey(ip), properties.getLoginIpFailures());
    }

    @Override
    public void recordLoginFailure(String username, String ip) {
        increment(userIpKey(normalize(username), ip));
        increment(ipKey(ip));
    }

    @Override
    public void recordLoginSuccess(String username, String ip) {
        try {
            redisTemplate.delete(userIpKey(normalize(username), ip));
        } catch (RuntimeException failure) {
            log.warn("清理 GameSave 登录限流计数失败，requestId={}", MDC.get("requestId"), failure);
        }
    }

    @Override
    public void assertAndRecordRegistrationAllowed(String ip) {
        String key = PREFIX + "register:ip:" + safe(ip);
        assertBelow(key, properties.getRegisterIpAttempts());
        increment(key);
    }

    private void assertBelow(String key, int limit) {
        try {
            String raw = redisTemplate.opsForValue().get(key);
            long count = raw == null ? 0L : Long.parseLong(raw);
            if (count >= Math.max(1, limit)) {
                throw GameSaveException.tooManyRequests(
                        "LOGIN_RATE_LIMITED", "请求过于频繁，请稍后再试", retryAfterSeconds(key));
            }
        } catch (GameSaveException limited) {
            throw limited;
        } catch (RuntimeException failure) {
            log.warn("读取 GameSave 认证限流计数失败，requestId={}", MDC.get("requestId"), failure);
        }
    }

    private void increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value != null && value == 1L) {
                redisTemplate.expire(
                        key, Duration.ofMinutes(Math.max(1, properties.getLoginRateLimitMinutes())));
            }
        } catch (RuntimeException failure) {
            log.warn("更新 GameSave 认证限流计数失败，requestId={}", MDC.get("requestId"), failure);
        }
    }

    private int retryAfterSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl == null || ttl <= 0 ? 1 : (int) Math.min(Integer.MAX_VALUE, ttl);
    }

    private String userIpKey(String username, String ip) {
        return PREFIX + "login:user-ip:" + username + ":" + safe(ip);
    }

    private String ipKey(String ip) {
        return PREFIX + "login:ip:" + safe(ip);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9_.:-]", "_");
    }
}
