package com.thx.module.payment.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.payment.domain.PaymentOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.Date;

public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    /**
     * 唯一的状态/退款金额原子更新入口：以 (paymentNo, expectedVersion) 做乐观锁条件，
     * 命中则 version+1，返回受影响行数（0 表示版本已被并发修改，调用方需要重新加载后重试或视为竞争失败）。
     * 在实际调用路径中，本方法总是在已经对该行持有 {@code SELECT ... FOR UPDATE} 行锁的事务内执行，
     * version 校验是双重保险而非唯一的并发保护手段。
     */
    @Update("UPDATE payment_order SET status=#{status}, success_time=#{successTime}, close_time=#{closeTime}, "
            + "refunded_amount=#{refundedAmount}, version=version+1, update_time=NOW() "
            + "WHERE payment_no=#{paymentNo} AND version=#{expectedVersion}")
    int casUpdate(@Param("paymentNo") String paymentNo,
                  @Param("status") String status,
                  @Param("successTime") Date successTime,
                  @Param("closeTime") Date closeTime,
                  @Param("refundedAmount") BigDecimal refundedAmount,
                  @Param("expectedVersion") Integer expectedVersion);
}
