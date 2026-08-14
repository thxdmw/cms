package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.module.admin.entity.Role;
import org.apache.ibatis.annotations.Param;

import java.util.Map;
import java.util.Set;

/**
 * 系统角色 Mapper。
 */
public interface RoleMapper extends BaseMapper<Role> {
    /**
     * 根据用户 id 查询该用户拥有的角色 id 集合。
     *
     * @param userId 用户id
     * @return 角色 id 集合
     */
    Set<String> findRoleByUserId(String userId);

    /**
     * 分页查询有效（status=1）角色列表，支持按角色名称模糊匹配。
     *
     * @param page 分页参数
     * @param role 查询条件：name 为模糊匹配（LIKE %xxx%），为空则不参与过滤
     * @return 分页结果
     */
    IPage<Role> selectRoles(@Param("page") IPage<Role> page, @Param("role") Role role);

    /**
     * 批量更新角色状态。
     *
     * @param params 参数 Map，需包含 key："status"（目标状态）、"roleIds"（角色 id 集合，用于 IN 条件过滤）
     * @return 影响行数
     */
    int updateStatusBatch(Map<String, Object> params);

    /**
     * 根据角色 id 更新角色的名称、描述。
     *
     * @param params 参数 Map，需包含 key："role_id"（角色 id）、"name"（角色名称）、"description"（角色描述）
     * @return 影响行数
     */
    int updateByRoleId(Map<String, Object> params);


}