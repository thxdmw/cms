package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.admin.entity.RolePermission;

/**
 * 角色-权限关联表 Mapper，维护角色（Role）与权限（Permission）之间的多对多关系。
 * 未定义自定义查询方法，增删改查直接使用 MyBatis-Plus 提供的通用 BaseMapper 能力。
 */
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}