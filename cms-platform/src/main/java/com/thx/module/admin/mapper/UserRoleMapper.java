package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.admin.entity.UserRole;

/**
 * 用户-角色关联表 Mapper，维护用户（User）与角色（Role）之间的多对多关系。
 * 未定义自定义查询方法，增删改查直接使用 MyBatis-Plus 提供的通用 BaseMapper 能力。
 */
public interface UserRoleMapper extends BaseMapper<UserRole> {
}