package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.admin.entity.SysConfig;

/**
 * 系统配置表 Mapper，对应键值对形式的系统参数配置项。
 * 未定义自定义查询方法，增删改查直接使用 MyBatis-Plus 提供的通用 BaseMapper 能力。
 */
public interface SysConfigMapper extends BaseMapper<SysConfig> {
}