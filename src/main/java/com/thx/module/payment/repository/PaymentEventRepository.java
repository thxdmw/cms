package com.thx.module.payment.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.payment.domain.PaymentEvent;
import com.thx.module.payment.domain.PaymentEventStatus;
import com.thx.module.payment.repository.mapper.PaymentEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PaymentEventRepository {

    private final PaymentEventMapper mapper;

    /**
     * 尝试插入事件，(aggregateType, aggregateId, eventType) 唯一约束冲突时返回 false，
     * 代表同一笔支付/退款的这类事件已经存在——无论触发来源是 Notify 还是 Query，都不会产生第二条。
     */
    public boolean insertIfAbsent(PaymentEvent event) {
        try {
            mapper.insert(event);
            return true;
        } catch (DuplicateKeyException e) {
            log.info("支付事件已存在，跳过重复创建: aggregateType={}, aggregateId={}, eventType={}",
                    event.getAggregateType(), event.getAggregateId(), event.getEventType());
            return false;
        }
    }

    public PaymentEvent findByEventId(String eventId) {
        return mapper.selectOne(new LambdaQueryWrapper<PaymentEvent>()
                .eq(PaymentEvent::getEventId, eventId));
    }

    public PaymentEvent findById(Long id) {
        return mapper.selectById(id);
    }

    /**
     * CAS 声明事件用于投递，见 {@link PaymentEventMapper#claim}。
     */
    public boolean claim(Long id, PaymentEventStatus expected, PaymentEventStatus target) {
        return mapper.claim(id, expected.name(), target.name()) == 1;
    }

    /**
     * 声明成功后的后续状态更新（PUBLISHING -&gt; PUBLISHED/FAILED），此时只有声明者本身在操作该行，
     * 不再需要 CAS。
     */
    public void update(PaymentEvent event) {
        mapper.updateById(event);
    }

    /**
     * 待投递事件：PENDING，或到期重试的 FAILED。
     */
    public List<PaymentEvent> findDueForDelivery(Date now, int limit) {
        return mapper.selectList(new LambdaQueryWrapper<PaymentEvent>()
                .eq(PaymentEvent::getStatus, PaymentEventStatus.PENDING.name())
                .or(w -> w.eq(PaymentEvent::getStatus, PaymentEventStatus.FAILED.name())
                        .le(PaymentEvent::getNextRetryTime, now))
                .orderByAsc(PaymentEvent::getCreateTime)
                .last("LIMIT " + limit));
    }
}
