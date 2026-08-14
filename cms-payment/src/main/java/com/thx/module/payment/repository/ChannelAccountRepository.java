package com.thx.module.payment.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.payment.domain.ChannelAccount;
import com.thx.module.payment.repository.mapper.ChannelAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChannelAccountRepository {

    private final ChannelAccountMapper mapper;

    public ChannelAccount findById(Long id) {
        return mapper.selectById(id);
    }

    public ChannelAccount findByAccountCode(String accountCode) {
        return mapper.selectOne(new LambdaQueryWrapper<ChannelAccount>()
                .eq(ChannelAccount::getAccountCode, accountCode));
    }
}
