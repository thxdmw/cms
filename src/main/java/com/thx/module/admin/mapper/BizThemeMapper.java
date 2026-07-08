package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.admin.entity.BizTheme;

/**
 * 前台主题（皮肤）Mapper。同一时间约定只有一个主题处于启用（status=1）状态。
 */
public interface BizThemeMapper extends BaseMapper<BizTheme> {

    /**
     * 将当前所有已启用（status=1）的主题批量置为未启用（status=0）。
     * 一般在切换主题前调用，配合 {@link #updateStatusById(String)} 保证全库同时只有一个启用中的主题。
     *
     * @return 影响行数
     */
    int setInvaid();

    /**
     * 将指定 id 的主题状态置为启用（status=1），用于切换/启用某个主题。
     *
     * @param id 主题 id
     * @return 影响行数
     */
    int updateStatusById(String id);

    /**
     * 根据 id 数组批量删除主题。
     *
     * @param ids 主题 id 数组
     * @return 影响行数
     */
    int deleteBatch(String[] ids);
}