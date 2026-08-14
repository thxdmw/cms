package com.thx.module.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.common.util.CoreConst;
import com.thx.module.admin.mapper.BizThemeMapper;
import com.thx.module.admin.entity.BizTheme;
import com.thx.module.admin.service.BizThemeService;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 前台主题（皮肤）服务实现。同一时间约定只有一个主题处于启用状态。
 */
@Service
@AllArgsConstructor
public class BizThemeServiceImpl extends ServiceImpl<BizThemeMapper, BizTheme> implements BizThemeService {

    private final BizThemeMapper themeMapper;

    /**
     * 切换启用主题：先将所有主题置为未启用，再启用指定 id 的主题，成功后清空主题缓存。
     * 注意：两步更新未加 {@code @Transactional} 保护，非原子操作。
     */
    @Override
    @CacheEvict(value = "theme", allEntries = true)
    public int useTheme(String id) {
        themeMapper.setInvaid();
        return themeMapper.updateStatusById(id);
    }

    /** 查询当前启用中的主题（status=1），缓存固定 key "current"。 */
    @Override
    @Cacheable(value = "theme", key = "'current'")
    public BizTheme selectCurrent() {
        return themeMapper.selectOne(Wrappers.<BizTheme>lambdaQuery().eq(BizTheme::getStatus, CoreConst.STATUS_VALID));
    }

    /** 批量删除主题，成功后清空主题缓存。 */
    @Override
    @CacheEvict(value = "theme", allEntries = true)
    public int deleteBatch(String[] ids) {
        return themeMapper.deleteBatch(ids);
    }
}
