package com.thx.module.payment.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.payment.domain.RefundOrder;
import com.thx.module.payment.domain.RefundStatus;
import com.thx.module.payment.repository.mapper.RefundOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
@RequiredArgsConstructor
public class RefundOrderRepository {

    private final RefundOrderMapper mapper;

    public void insert(RefundOrder refundOrder) {
        mapper.insert(refundOrder);
    }

    public RefundOrder findByRefundNo(String refundNo) {
        return mapper.selectOne(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getRefundNo, refundNo));
    }

    public RefundOrder findByAppCodeAndBusinessRefundNo(String appCode, String businessRefundNo) {
        return mapper.selectOne(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getAppCode, appCode)
                .eq(RefundOrder::getBusinessRefundNo, businessRefundNo));
    }

    /**
     * 原子更新退款单状态，调用方必须传入锁定读取时拿到的 version（见 {@link PaymentOrderRepository#casUpdate}）。
     */
    public boolean casUpdate(String refundNo, int expectedVersion, RefundStatus status, String channelRefundNo,
                              String failureCode, String failureMessage, Date successTime) {
        int rows = mapper.casUpdate(refundNo, status.name(), channelRefundNo, failureCode, failureMessage,
                successTime, expectedVersion);
        return rows == 1;
    }
}
