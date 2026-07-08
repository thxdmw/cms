package com.thx.module.admin.service.impl;

import com.thx.module.admin.mapper.BizArticleMapper;
import com.thx.module.admin.service.BizSiteInfoService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 站点概览信息服务实现，直接委托 {@link BizArticleMapper#getSiteInfo()} 完成统计。
 */
@Service
@AllArgsConstructor
public class BizSiteInfoServiceImpl implements BizSiteInfoService {

    private final BizArticleMapper bizArticleMapper;

    @Override
    public Map<String, Object> getSiteInfo() {
        return bizArticleMapper.getSiteInfo();
    }

}
