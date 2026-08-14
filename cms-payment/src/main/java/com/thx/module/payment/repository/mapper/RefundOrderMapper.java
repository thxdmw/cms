package com.thx.module.payment.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.payment.domain.RefundOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

public interface RefundOrderMapper extends BaseMapper<RefundOrder> {

    /**
     * 退款单状态原子更新，语义同 {@link PaymentOrderMapper#casUpdate}。
     */
    @Update("UPDATE payment_refund_order SET status=#{status}, channel_refund_no=#{channelRefundNo}, "
            + "failure_code=#{failureCode}, failure_message=#{failureMessage}, success_time=#{successTime}, "
            + "version=version+1, update_time=NOW() WHERE refund_no=#{refundNo} AND version=#{expectedVersion}")
    int casUpdate(@Param("refundNo") String refundNo,
                  @Param("status") String status,
                  @Param("channelRefundNo") String channelRefundNo,
                  @Param("failureCode") String failureCode,
                  @Param("failureMessage") String failureMessage,
                  @Param("successTime") Date successTime,
                  @Param("expectedVersion") Integer expectedVersion);
}
