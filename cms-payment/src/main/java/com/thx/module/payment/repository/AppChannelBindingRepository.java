package com.thx.module.payment.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.payment.api.enums.PaymentChannel;
import com.thx.module.payment.api.enums.PaymentScene;
import com.thx.module.payment.domain.AppChannelBinding;
import com.thx.module.payment.repository.mapper.AppChannelBindingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AppChannelBindingRepository {

    private final AppChannelBindingMapper mapper;

    /**
     * 按 (appCode, channel, scene) 查启用中、priority 最小的一条绑定；不存在返回 null。
     */
    public AppChannelBinding findBestBinding(String appCode, PaymentChannel channel, PaymentScene scene) {
        List<AppChannelBinding> candidates = mapper.selectList(new LambdaQueryWrapper<AppChannelBinding>()
                .eq(AppChannelBinding::getAppCode, appCode)
                .eq(AppChannelBinding::getChannel, channel.name())
                .eq(AppChannelBinding::getScene, scene.name())
                .eq(AppChannelBinding::getEnabled, 1)
                .orderByAsc(AppChannelBinding::getPriority)
                .last("LIMIT 1"));
        return candidates.isEmpty() ? null : candidates.get(0);
    }
}
