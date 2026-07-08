package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.admin.entity.Permission;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 权限（资源）Mapper。权限按 type 区分为 0-目录、1-菜单、2-按钮三种类型，
 * 通过 parent_id 形成树形结构，并与角色（Role）通过 role_permission 建立多对多关系。
 */
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据状态查询全部权限资源（目录/菜单/按钮均包含），按 order_num 升序排列。
     *
     * @param status 状态：1 有效，0 无效
     * @return 权限列表
     */
    List<Permission> selectAllPerms(Integer status);

    /**
     * 根据状态查询全部菜单类资源（type 为 0-目录 或 1-菜单，不含按钮），按 order_num 升序排列。
     *
     * @param status 状态：1 有效，0 无效
     * @return 权限列表（仅含 id、permissionId、name、parentId、type 字段）
     */
    List<Permission> selectAllMenuName(Integer status);

    /**
     * 根据用户 id 查询该用户（经由所属角色）拥有的权限标识（perms）集合，
     * 仅统计 perms 不为空且状态有效（status=1）的权限。
     *
     * @param userId 用户id
     * @return 权限标识集合，用于 Shiro 鉴权
     */
    Set<String> findPermsByUserId(String userId);

    /**
     * 根据角色 id 查询该角色被授予的权限列表（仅状态有效的权限，只返回 permissionId、name、parentId）。
     *
     * @param id 角色id
     * @return 权限列表
     */
    List<Permission> findByRoleId(String id);

    /**
     * 根据用户 id 查询该用户（经由所属角色）拥有的全部有效权限（去重）。
     *
     * @param userId 用户id
     * @return 权限列表
     */
    List<Permission> selectByUserId(String userId);

    /**
     * 根据用户 id 查询该用户可见的菜单（type 为 0-目录 或 1-菜单，且状态有效），按 order_num 升序排列，用于渲染左侧导航菜单。
     *
     * @param userId 用户id
     * @return 菜单列表
     */
    List<Permission> selectMenuByUserId(String userId);

    /**
     * 根据权限 id 修改其状态。
     *
     * @param permissionId 权限id
     * @param status       目标状态：1 有效，0 无效
     * @return 影响行数
     */
    int updateStatusByPermissionId(@Param("permissionId") String permissionId, @Param("status") Integer status);

    /**
     * 根据权限 id 查询权限详情。
     *
     * @param permissionId 权限id
     * @return 权限详情
     */
    Permission selectByPermissionId(String permissionId);

    /**
     * 根据权限对象更新其 name、description、url、perms、parentId、orderNum、icon（以 permissionId 定位记录）。
     *
     * @param permission 权限（需携带 permissionId 及待更新字段）
     * @return 影响行数
     */
    int updateByPermissionId(Permission permission);

    /**
     * 统计指定权限下、状态有效的直接子资源数量，常用于判断该权限能否被删除（存在子资源时禁止删除）。
     *
     * @param permissionId 权限id
     * @return 子资源数量
     */
    int selectSubPermsByPermissionId(String permissionId);
}