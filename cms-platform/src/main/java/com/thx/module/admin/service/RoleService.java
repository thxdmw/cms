package com.thx.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.Permission;
import com.thx.module.admin.entity.Role;
import com.thx.module.admin.entity.User;

import java.util.List;
import java.util.Set;

/**
 * 系统角色服务，维护角色信息及角色与权限、角色与用户之间的关联关系。
 */
public interface RoleService extends IService<Role> {

    /**
     * 查询用户拥有的角色 id 集合。
     *
     * @param userId 用户id
     * @return 角色 id 集合
     */
    Set<String> findRoleByUserId(String userId);

    /**
     * 分页查询有效角色列表，支持按名称模糊匹配。
     *
     * @param role       查询条件：name 模糊匹配
     * @param pageNumber 页码
     * @param pageSize   每页条数
     * @return 分页结果
     */
    IPage<Role> selectRoles(Role role, Integer pageNumber, Integer pageSize);

    /**
     * 新增角色，自动生成 roleId、置为有效状态并补全创建时间。
     *
     * @param role 待新增角色
     * @return 影响行数
     */
    int insert(Role role);

    /**
     * 批量更新角色状态。
     *
     * @param roleIds 角色 id 集合
     * @param status  目标状态
     * @return 影响行数
     */
    int updateStatusBatch(List<String> roleIds, Integer status);

    /**
     * 根据角色 id（业务主键 roleId）查询角色详情。
     *
     * @param roleId 角色id
     * @return 角色详情
     */
    Role findById(String roleId);

    /**
     * 根据角色 id 更新角色的名称、描述。
     *
     * @param role 角色（需携带 roleId、name、description）
     * @return 影响行数
     */
    int updateByRoleId(Role role);

    /**
     * 查询角色被授予的权限列表。
     *
     * @param roleId 角色id
     * @return 权限列表
     */
    List<Permission> findPermissionsByRoleId(String roleId);

    /**
     * 为角色重新分配权限：先清空该角色现有的全部权限关联，再逐条插入新的权限关联。
     *
     * @param roleId            角色id
     * @param permissionIdsList 新的权限 id 列表
     */
    void addAssignPermission(String roleId, List<String> permissionIdsList);

    /**
     * 查询拥有指定角色的用户列表。
     *
     * @param roleId 角色id
     * @return 用户列表
     */
    List<User> findByRoleId(String roleId);

    /**
     * 查询拥有指定角色集合中任一角色的用户列表。
     *
     * @param roleIds 角色 id 集合
     * @return 用户列表
     */
    List<User> findByRoleIds(List<String> roleIds);


}
