package com.thx.module.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.common.util.CoreConst;
import com.thx.common.util.UUIDUtil;
import com.thx.module.admin.mapper.PermissionMapper;
import com.thx.module.admin.entity.Permission;
import com.thx.module.admin.service.PermissionService;
import lombok.AllArgsConstructor;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@link PermissionService} 实现：权限/菜单的增改查，以及把扁平权限列表组装成树形结构
 * （buildPermissionTree，供后台侧边栏菜单和权限管理树形展示复用）。
 */
@Service
@AllArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private static final Pattern SLASH_PATTERN = Pattern.compile("/");
    /**
     * 权限树顶层节点的 parent_id 取值。Permission.parentId 是 String（UUID 主键改造后），
     * 不能复用 CoreConst.TOP_MENU_ID（Integer，Category 等其它模块仍在用），需要单独定义
     */
    private static final String TOP_LEVEL_PARENT_ID = "0";
    private final PermissionMapper permissionMapper;

    @Override
    public Set<String> findPermsByUserId(String userId) {
        return permissionMapper.findPermsByUserId(userId);
    }

    @Override
    public List<Permission> selectAll(Integer status) {
        return permissionMapper.selectAllPerms(status);
    }

    @Override
    public List<Permission> selectAllMenuName(Integer status) {
        return permissionMapper.selectAllMenuName(status);
    }

    @Override
    public List<Permission> selectMenuByUserId(String userId) {
        return permissionMapper.selectMenuByUserId(userId);
    }

    @Override
    public List<Permission> selectMenuTreeByUserId(String userId) {
        return buildPermissionTree(permissionMapper.selectMenuByUserId(userId));
    }

    @Override
    public int insert(Permission permission) {
        Date date = new Date();
        permission.setPermissionId(UUIDUtil.getUniqueIdByUUId());
        permission.setStatus(CoreConst.STATUS_VALID);
        permission.setCreateTime(date);
        permission.setUpdateTime(date);
        return permissionMapper.insert(permission);
    }

    @Override
    public int updateStatus(String permissionId, Integer status) {
        return permissionMapper.updateStatusByPermissionId(permissionId, status);
    }

    @Override
    public Permission findByPermissionId(String permissionId) {
        return permissionMapper.selectByPermissionId(permissionId);
    }

    @Override
    public Permission findById(String id) {
        return permissionMapper.selectById(id);
    }

    @Override
    public int updateByPermissionId(Permission permission) {
        return permissionMapper.updateByPermissionId(permission);
    }

    @Override
    public int selectSubPermsByPermissionId(String permissionId) {
        return permissionMapper.selectSubPermsByPermissionId(permissionId);
    }

    private static List<Permission> buildPermissionTree(List<Permission> permissionList) {
        Map<String, List<Permission>> parentIdToPermissionListMap = permissionList.stream().peek(p -> {
            if (StrUtil.startWith(p.getUrl(), "/")) {
                p.setUrl(SLASH_PATTERN.matcher(p.getUrl()).replaceFirst("#"));
            }
        }).collect(Collectors.groupingBy(Permission::getParentId));
        List<Permission> rootLevelPermissionList = parentIdToPermissionListMap.getOrDefault(TOP_LEVEL_PARENT_ID, Collections.emptyList());
        fetchChildren(rootLevelPermissionList, parentIdToPermissionListMap);
        return rootLevelPermissionList;
    }

    private static void fetchChildren(List<Permission> permissionList, Map<String, List<Permission>> parentIdToPermissionListMap) {
        if (CollUtil.isEmpty(permissionList)) {
            return;
        }
        for (Permission permission : permissionList) {
            List<Permission> childrenList = parentIdToPermissionListMap.get(permission.getId());
            fetchChildren(childrenList, parentIdToPermissionListMap);
            permission.setChildren(childrenList);
        }
    }
}
