package com.thx.module.payment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thx.module.payment.domain.PaymentAuditAction;
import com.thx.module.payment.domain.PaymentAuditLog;
import com.thx.module.payment.infrastructure.SensitiveDataMasker;
import com.thx.module.payment.repository.PaymentAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 支付审计日志写入，detail 落库前统一经 {@link SensitiveDataMasker} 脱敏。
 * 审计写入失败不能影响主业务流程，异常仅记录日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAuditLogService {

    private final PaymentAuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public void record(PaymentAuditAction action, String appCode, String paymentNo, String attemptNo,
                        String refundNo, Map<String, Object> detail, String requestId) {
        try {
            PaymentAuditLog log = new PaymentAuditLog()
                    .setAppCode(appCode)
                    .setPaymentNo(paymentNo)
                    .setAttemptNo(attemptNo)
                    .setRefundNo(refundNo)
                    .setAction(action.name())
                    .setDetail(detail == null ? null : objectMapper.writeValueAsString(SensitiveDataMasker.mask(detail)))
                    .setRequestId(requestId);
            repository.insert(log);
        } catch (Exception e) {
            log.warn("写入支付审计日志失败: action={}, paymentNo={}", action, paymentNo, e);
        }
    }

    public void record(PaymentAuditAction action, String appCode, String paymentNo) {
        record(action, appCode, paymentNo, null, null, null, null);
    }

    public void record(PaymentAuditAction action, String appCode, String paymentNo, Map<String, Object> detail) {
        record(action, appCode, paymentNo, null, null, detail, null);
    }
}
