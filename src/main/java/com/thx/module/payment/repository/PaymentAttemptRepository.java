package com.thx.module.payment.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.payment.domain.PaymentAttempt;
import com.thx.module.payment.domain.PaymentAttemptStatus;
import com.thx.module.payment.repository.mapper.PaymentAttemptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PaymentAttemptRepository {

    private final PaymentAttemptMapper mapper;

    public void insert(PaymentAttempt attempt) {
        mapper.insert(attempt);
    }

    public PaymentAttempt findByAttemptNo(String attemptNo) {
        return mapper.selectOne(new LambdaQueryWrapper<PaymentAttempt>()
                .eq(PaymentAttempt::getAttemptNo, attemptNo));
    }

    public PaymentAttempt findLatestByPaymentNo(String paymentNo) {
        List<PaymentAttempt> list = mapper.selectList(new LambdaQueryWrapper<PaymentAttempt>()
                .eq(PaymentAttempt::getPaymentNo, paymentNo)
                .orderByDesc(PaymentAttempt::getCreateTime)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 退款时需要知道当初是用哪个 channelAccountId/attemptNo(out_trade_no) 支付成功的。
     */
    public PaymentAttempt findSuccessAttempt(String paymentNo) {
        List<PaymentAttempt> list = mapper.selectList(new LambdaQueryWrapper<PaymentAttempt>()
                .eq(PaymentAttempt::getPaymentNo, paymentNo)
                .eq(PaymentAttempt::getStatus, PaymentAttemptStatus.SUCCESS.name())
                .orderByDesc(PaymentAttempt::getUpdateTime)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 更新一条已存在的 Attempt。所有实际调用路径都发生在已持有对应 {@code PaymentOrder} 行锁的事务内
     * （见 {@code PaymentChannelResultProcessor}），或作用于刚插入、attemptNo 全局唯一的新记录
     * （见创建支付流程），因此不需要额外的乐观锁保护。
     */
    public void update(PaymentAttempt attempt) {
        mapper.updateById(attempt);
    }

    /**
     * 供 {@code PaymentReconcileScheduler} 扫描需要主动查询澄清的记录：结果未知的；
     * 长时间停留在 PROCESSING 的；长时间停留在 INIT 的（服务在调用渠道后、保存首次结果前崩溃，
     * 见 docs/payment-architecture.md 并发场景清单"支付宝创建支付调用成功但服务崩溃"）。
     */
    public List<PaymentAttempt> findNeedsReconcile(Date staleBefore, int limit) {
        return mapper.selectList(new LambdaQueryWrapper<PaymentAttempt>()
                .eq(PaymentAttempt::getStatus, PaymentAttemptStatus.UNKNOWN.name())
                .or(w -> w.eq(PaymentAttempt::getStatus, PaymentAttemptStatus.PROCESSING.name())
                        .lt(PaymentAttempt::getUpdateTime, staleBefore))
                .or(w -> w.eq(PaymentAttempt::getStatus, PaymentAttemptStatus.INIT.name())
                        .lt(PaymentAttempt::getUpdateTime, staleBefore))
                .orderByAsc(PaymentAttempt::getUpdateTime)
                .last("LIMIT " + limit));
    }
}
