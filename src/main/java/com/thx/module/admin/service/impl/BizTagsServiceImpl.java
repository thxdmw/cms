package com.thx.module.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.common.util.Pagination;
import com.thx.module.admin.mapper.BizTagsMapper;
import com.thx.module.admin.entity.BizTags;
import com.thx.module.admin.service.BizTagsService;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 文章标签服务实现。
 */
@Service
@AllArgsConstructor
public class BizTagsServiceImpl extends ServiceImpl<BizTagsMapper, BizTags> implements BizTagsService {

    private final BizTagsMapper bizTagsMapper;

    /** 查询全部标签（不分页），缓存固定 key "list"。 */
    @Override
    @Cacheable(value = "tag", key = "'list'")
    public List<BizTags> selectTags(BizTags bizTags) {
        return bizTagsMapper.selectTags(null, bizTags);
    }

    /** 分页查询标签列表（不走缓存）。 */
    @Override
    public IPage<BizTags> pageTags(BizTags bizTags, Integer pageNumber, Integer pageSize) {
        IPage<BizTags> page = new Pagination<>(pageNumber, pageSize);
        return page.setRecords(bizTagsMapper.selectTags(page, bizTags));
    }

    /** 批量删除标签（走 MyBatis-Plus 通用的 deleteBatchIds），成功后清空标签缓存。 */
    @Override
    @CacheEvict(value = "tag", allEntries = true)
    public int deleteBatch(String[] ids) {
        return bizTagsMapper.deleteBatchIds(Arrays.asList(ids));
    }
}
