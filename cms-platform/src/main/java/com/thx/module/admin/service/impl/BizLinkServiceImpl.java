package com.thx.module.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.common.util.Pagination;
import com.thx.module.admin.mapper.BizLinkMapper;
import com.thx.module.admin.entity.BizLink;
import com.thx.module.admin.service.BizLinkService;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 友情链接服务实现。
 */
@Service
@AllArgsConstructor
public class BizLinkServiceImpl extends ServiceImpl<BizLinkMapper, BizLink> implements BizLinkService {

    private final BizLinkMapper linkMapper;

    /** 查询全部友链（不分页），缓存固定 key "list"。 */
    @Override
    @Cacheable(value = "link", key = "'list'")
    public List<BizLink> selectLinks(BizLink bizLink) {
        return linkMapper.selectLinks(null, bizLink);
    }

    /** 分页查询友链列表（不走缓存）。 */
    @Override
    public IPage<BizLink> pageLinks(BizLink bizLink, Integer pageNumber, Integer pageSize) {
        IPage<BizLink> page = new Pagination<>(pageNumber, pageSize);
        page.setRecords(linkMapper.selectLinks(page, bizLink));
        return page;
    }

    /** 批量删除友链，成功后清空友链缓存。 */
    @Override
    @CacheEvict(value = "link", allEntries = true)
    public int deleteBatch(String[] ids) {
        return linkMapper.deleteBatch(ids);
    }

}
