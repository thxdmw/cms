package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.admin.entity.BizTheme;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public interface BizThemeMapper extends BaseMapper<BizTheme> {

    int setInvaid();

    int updateStatusById(String id);

    int deleteBatch(String[] ids);
}