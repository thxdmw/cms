package com.thx.module.payment.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.payment.domain.PaymentBusinessApp;
import com.thx.module.payment.repository.mapper.PaymentBusinessAppMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentBusinessAppRepository {

    private final PaymentBusinessAppMapper mapper;

    public PaymentBusinessApp findByAppCode(String appCode) {
        return mapper.selectOne(new LambdaQueryWrapper<PaymentBusinessApp>()
                .eq(PaymentBusinessApp::getAppCode, appCode));
    }
}
