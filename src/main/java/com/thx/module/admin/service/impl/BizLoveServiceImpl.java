package com.thx.module.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.module.admin.mapper.BizLoveMapper;
import com.thx.module.admin.entity.BizLove;
import com.thx.module.admin.service.BizLoveService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 点赞服务实现。
 */
@Service
@AllArgsConstructor
public class BizLoveServiceImpl extends ServiceImpl<BizLoveMapper, BizLove> implements BizLoveService {

    private final BizLoveMapper loveMapper;

    /** 按业务对象 id + 用户 IP 查询点赞记录，判断是否已点过赞。 */
    @Override
    public BizLove checkLove(String bizId, String userIp) {
        return loveMapper.checkLove(bizId, userIp);
    }
}
