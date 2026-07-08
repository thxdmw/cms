package com.thx.module.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.module.admin.mapper.BizCategoryMapper;
import com.thx.module.admin.entity.BizCategory;
import com.thx.module.admin.service.BizCategoryService;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文章分类服务实现。
 */
@Service
@AllArgsConstructor
public class BizCategoryServiceImpl extends ServiceImpl<BizCategoryMapper, BizCategory> implements BizCategoryService {

    private final BizCategoryMapper bizCategoryMapper;

    /** 查询分类列表（含父子关联），缓存固定 key "tree"。 */
    @Override
    @Cacheable(value = "category", key = "'tree'")
    public List<BizCategory> selectCategories(BizCategory bizCategory) {
        return bizCategoryMapper.selectCategories(bizCategory);
    }

    /** 批量删除分类，成功后清空分类缓存。 */
    @Override
    @CacheEvict(value = "category", allEntries = true)
    public int deleteBatch(String[] ids) {
        return bizCategoryMapper.deleteBatch(ids);
    }

    /** 查询分类详情（含父分类基本信息）。 */
    @Override
    public BizCategory selectById(String id) {
        return bizCategoryMapper.getById(id);
    }

    /** 按父分类 id 精确查询直接子分类列表。 */
    @Override
    public List<BizCategory> selectByPid(String pid) {
        return bizCategoryMapper.selectList(Wrappers.<BizCategory>lambdaQuery().eq(BizCategory::getPid, pid));
    }
}
