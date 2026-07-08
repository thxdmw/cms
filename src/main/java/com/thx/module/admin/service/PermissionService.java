package com.thx.module.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.Permission;

import java.util.List;
import java.util.Set;

/**
 * 权限（资源）服务。权限按 type 区分为 0-目录、1-菜单、2-按钮，通过 parentId 形成树形结构，
 * 是 Shiro 鉴权体系中权限标识（perms）的来源。
 */
public interface PermissionService extends IService<Permission> {

    /**
     * 查询用户（经由所属角色）拥有的权限标识（perms）集合，供 Shiro 鉴权使用。
     *
     * @param userId 用户id
     * @return 权限标识集合
     */
    Set<String> findPermsByUserId(String userId);

    /**
     * 按状态查询全部权限资源（目录/菜单/按钮均包含），按排序号升序排列。
     *
     * @param status 状态：1 有效，0 无效
     * @return 权限列表
     */
    List<Permission> selectAll(Integer status);

    /**
     * 按状态查询全部菜单类资源（目录、菜单，不含按钮）。
     *
     * @param status 状态：1 有效，0 无效
     * @return 权限列表（精简字段）
     */
    List<Permission> selectAllMenuName(Integer status);

    /**
     * 查询用户可见的菜单（目录、菜单，不含按钮）扁平列表，按排序号升序排列。
     *
     * @param userId 用户id
     * @return 菜单列表（扁平结构）
     */
    List<Permission> selectMenuByUserId(String userId);

    /**
     * 查询用户可见的菜单，并在内存中按 parentId 组装为树形结构（顶层节点 parentId 为 "0"），
     * 同时会把以 "/" 开头的 url 中的首个 "/" 替换为 "#"（适配前端路由）。
     *
     * @param userId 用户id
     * @return 菜单树（仅含用户可见的顶层节点及其子节点）
     */
    List<Permission> selectMenuTreeByUserId(String userId);

    /**
     * 新增权限，自动生成 permissionId、置为有效状态并补全创建/更新时间。
     *
     * @param permission 待新增权限
     * @return 影响行数
     */
    int insert(Permission permission);

    /**
     * 根据权限 id 更新其状态（启用/禁用）。
     *
     * @param permissionId 权限id
     * @param status       目标状态：1 有效，0 无效
     * @return 影响行数
     */
    int updateStatus(String permissionId, Integer status);

    /**
     * 根据权限 id（业务主键 permissionId）查询权限详情。
     *
     * @param permissionId 权限id
     * @return 权限详情
     */
    Permission findByPermissionId(String permissionId);

    /**
     * 根据主键 id 查询权限。
     *
     * @param id 主键 id
     * @return 权限详情
     */
    Permission findById(String id);

    /**
     * 根据权限对象更新 name、description、url、perms、parentId、orderNum、icon（以 permissionId 定位记录）。
     *
     * @param permission 权限（需携带 permissionId 及待更新字段）
     * @return 影响行数
     */
    int updateByPermissionId(Permission permission);

    /**
     * 统计指定权限下、状态有效的直接子资源数量，常用于删除前校验（存在子资源时禁止删除）。
     *
     * @param permissionId 权限id
     * @return 子资源数量
     */
    int selectSubPermsByPermissionId(String permissionId);
}
