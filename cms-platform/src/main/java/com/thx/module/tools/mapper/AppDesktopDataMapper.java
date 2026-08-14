package com.thx.module.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.tools.entity.AppDesktopData;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@link AppDesktopData}（应用桌面条目）的 MyBatis-Plus Mapper，无自定义 SQL，
 * 直接依赖 {@link BaseMapper} 提供的通用增删改查能力。
 */
@Mapper
public interface AppDesktopDataMapper extends BaseMapper<AppDesktopData> {
}
