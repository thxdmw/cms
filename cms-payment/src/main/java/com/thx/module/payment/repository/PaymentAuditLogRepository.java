package com.thx.module.payment.repository;

import com.thx.module.payment.domain.PaymentAuditLog;
import com.thx.module.payment.repository.mapper.PaymentAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentAuditLogRepository {

    private final PaymentAuditLogMapper mapper;

    public void insert(PaymentAuditLog log) {
        mapper.insert(log);
    }
}
