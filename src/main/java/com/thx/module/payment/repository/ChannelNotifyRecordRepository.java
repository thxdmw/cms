package com.thx.module.payment.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.payment.domain.ChannelNotifyRecord;
import com.thx.module.payment.repository.mapper.ChannelNotifyRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ChannelNotifyRecordRepository {

    private final ChannelNotifyRecordMapper mapper;

    /**
     * 尝试插入，notifyKey 唯一约束冲突时返回 false（代表这是一次重复通知），不抛异常。
     * 这是重复通知的第一道防线；最终幂等仍以 {@code PaymentOrder} 状态机 + {@code PaymentEvent} 唯一约束为准。
     */
    public boolean tryInsert(ChannelNotifyRecord record) {
        try {
            mapper.insert(record);
            return true;
        } catch (DuplicateKeyException e) {
            log.info("渠道通知重复到达，notifyKey={}", record.getNotifyKey());
            return false;
        }
    }

    public ChannelNotifyRecord findByNotifyKey(String notifyKey) {
        return mapper.selectOne(new LambdaQueryWrapper<ChannelNotifyRecord>()
                .eq(ChannelNotifyRecord::getNotifyKey, notifyKey));
    }

    public void update(ChannelNotifyRecord record) {
        mapper.updateById(record);
    }
}
