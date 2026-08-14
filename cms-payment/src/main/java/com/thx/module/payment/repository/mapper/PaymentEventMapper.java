package com.thx.module.payment.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.payment.domain.PaymentEvent;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface PaymentEventMapper extends BaseMapper<PaymentEvent> {

    /**
     * CAS 声明一个事件用于投递：只有当前 status 仍等于 expectedStatus 时才能声明成功，
     * 防止多个调度器实例/实时触发与定时补偿并发重复投递同一事件。
     */
    @Update("UPDATE payment_event SET status=#{newStatus}, update_time=NOW() "
            + "WHERE id=#{id} AND status=#{expectedStatus}")
    int claim(@Param("id") Long id, @Param("expectedStatus") String expectedStatus, @Param("newStatus") String newStatus);
}
