package com.thx.module.payment.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.payment.domain.PaymentOrder;
import com.thx.module.payment.domain.PaymentStatus;
import com.thx.module.payment.repository.mapper.PaymentOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;

/**
 * {@link PaymentOrder} 的唯一持久化入口。Application 层禁止直接注入 {@link PaymentOrderMapper}。
 */
@Repository
@RequiredArgsConstructor
public class PaymentOrderRepository {

    private final PaymentOrderMapper mapper;

    public void insert(PaymentOrder order) {
        mapper.insert(order);
    }

    public PaymentOrder findByPaymentNo(String paymentNo) {
        return mapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getPaymentNo, paymentNo));
    }

    public PaymentOrder findByAppCodeAndBusinessOrderNo(String appCode, String businessOrderNo) {
        return mapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getAppCode, appCode)
                .eq(PaymentOrder::getBusinessOrderNo, businessOrderNo));
    }

    /**
     * 加行锁读取，必须运行在已开启的事务内（{@code @Transactional}），否则 FOR UPDATE 不生效
     * 且连接归还后锁会被立即释放。
     */
    public PaymentOrder lockByPaymentNo(String paymentNo) {
        return mapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getPaymentNo, paymentNo)
                .last("FOR UPDATE"));
    }

    /**
     * 原子更新状态/退款金额，调用方必须传入锁定读取时拿到的 version。
     *
     * @return 是否更新成功；false 表示 version 已被并发修改（在已持有行锁的调用路径下理论上不会发生，
     * 出现即代表存在未通过行锁保护的写入路径，属于需要立即排查的缺陷）。
     */
    public boolean casUpdate(String paymentNo, int expectedVersion, PaymentStatus status,
                              Date successTime, Date closeTime, BigDecimal refundedAmount) {
        int rows = mapper.casUpdate(paymentNo, status.name(), successTime, closeTime, refundedAmount, expectedVersion);
        return rows == 1;
    }
}
