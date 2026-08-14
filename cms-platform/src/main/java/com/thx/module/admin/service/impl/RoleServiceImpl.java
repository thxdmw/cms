package com.thx.module.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.common.util.Pagination;
import com.thx.common.util.UUIDUtil;
import com.thx.module.admin.mapper.PermissionMapper;
import com.thx.module.admin.mapper.RoleMapper;
import com.thx.module.admin.mapper.RolePermissionMapper;
import com.thx.module.admin.mapper.UserMapper;
import com.thx.module.admin.entity.Permission;
import com.thx.module.admin.entity.Role;
import com.thx.module.admin.entity.RolePermission;
import com.thx.module.admin.entity.User;
import com.thx.module.admin.service.RoleService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * {@link RoleService} 实现：角色的增改查、角色与权限的分配关系维护（addAssignPermission）、
 * 按角色反查用户。
 */
@Service
@AllArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserMapper userMapper;

    @Override
    public Set<String> findRoleByUserId(String userId) {
        return roleMapper.findRoleByUserId(userId);
    }

    @Override
    public IPage<Role> selectRoles(Role role, Integer pageNumber, Integer pageSize) {
        IPage<Role> page = new Pagination<>(pageNumber, pageSize);
        return roleMapper.selectRoles(page, role);
    }

    @Override
    public int insert(Role role) {
        role.setRoleId(UUIDUtil.getUniqueIdByUUId());
        role.setStatus(1);
        role.setCreateTime(new Date());
        return roleMapper.insert(role);
    }

    @Override
    public int updateStatusBatch(List<String> roleIds, Integer status) {
        Map<String, Object> params = new HashMap<String, Object>(2);
        params.put("roleIds", roleIds);
        params.put("status", status);
        return roleMapper.updateStatusBatch(params);
    }

    @Override
    public Role findById(String roleId) {
        return roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getRoleId, roleId));
    }

    @Override
    public int updateByRoleId(Role role) {
        Map<String, Object> params = new HashMap<>(3);
        params.put("name", role.getName());
        params.put("description", role.getDescription());
        params.put("role_id", role.getRoleId());
        return roleMapper.updateByRoleId(params);
    }

    @Override
    public List<Permission> findPermissionsByRoleId(String roleId) {
        return permissionMapper.findByRoleId(roleId);
    }

    @Override
    public void addAssignPermission(String roleId, List<String> permissionIds) {
        rolePermissionMapper.delete(Wrappers.<RolePermission>lambdaQuery().eq(RolePermission::getRoleId, roleId));
        for (String permissionId : permissionIds) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            rolePermissionMapper.insert(rolePermission);
        }
    }

    @Override
    public List<User> findByRoleId(String roleId) {
        return userMapper.findByRoleId(roleId);
    }

    @Override
    public List<User> findByRoleIds(List<String> roleIds) {
        return userMapper.findByRoleIds(roleIds);
    }

}
