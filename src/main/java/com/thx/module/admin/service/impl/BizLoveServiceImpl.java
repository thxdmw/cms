package com.thx.module.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.module.admin.mapper.BizLoveMapper;
import com.thx.module.admin.entity.BizLove;
import com.thx.module.admin.service.BizLoveService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Service
@AllArgsConstructor
public class BizLoveServiceImpl extends ServiceImpl<BizLoveMapper, BizLove> implements BizLoveService {

    private final BizLoveMapper loveMapper;

    @Override
    public BizLove checkLove(String bizId, String userIp) {
        return loveMapper.checkLove(bizId, userIp);
    }
}
