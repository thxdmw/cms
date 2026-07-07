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
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Service
@AllArgsConstructor
public class BizCategoryServiceImpl extends ServiceImpl<BizCategoryMapper, BizCategory> implements BizCategoryService {

    private final BizCategoryMapper bizCategoryMapper;

    @Override
    @Cacheable(value = "category", key = "'tree'")
    public List<BizCategory> selectCategories(BizCategory bizCategory) {
        return bizCategoryMapper.selectCategories(bizCategory);
    }

    @Override
    @CacheEvict(value = "category", allEntries = true)
    public int deleteBatch(String[] ids) {
        return bizCategoryMapper.deleteBatch(ids);
    }

    @Override
    public BizCategory selectById(String id) {
        return bizCategoryMapper.getById(id);
    }

    @Override
    public List<BizCategory> selectByPid(String pid) {
        return bizCategoryMapper.selectList(Wrappers.<BizCategory>lambdaQuery().eq(BizCategory::getPid, pid));
    }
}
