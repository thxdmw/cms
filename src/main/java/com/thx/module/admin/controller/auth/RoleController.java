package com.thx.module.admin.controller.auth;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.common.shiro.MyShiroRealm;
import com.thx.common.util.CoreConst;
import com.thx.common.util.ResultUtil;
import com.thx.module.admin.entity.Permission;
import com.thx.module.admin.entity.Role;
import com.thx.module.admin.entity.User;
import com.thx.module.admin.service.PermissionService;
import com.thx.module.admin.service.RoleService;
import com.thx.module.admin.vo.PermissionTreeListVo;
import com.thx.module.admin.vo.base.PageResultVo;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 后台角色管理接口，对应前端 admin-app 的角色管理模块：角色列表、增删改、权限分配。
 */
@Slf4j
@Controller
@RequestMapping("/role")
@AllArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final MyShiroRealm myShiroRealm;


    /**
     * 角色列表数据（分页）
     */
    @PostMapping("/list")
    @ResponseBody
    public PageResultVo pageRoles(Role role, Integer pageNumber, Integer pageSize) {
        try {
            IPage<Role> rolePage = roleService.selectRoles(role, pageNumber, pageSize);
            return ResultUtil.table(rolePage.getRecords(), rolePage.getTotal());
        } catch (Exception e) {
            log.error(String.format("RoleController.loadRoles%s", e));
            throw e;
        }
    }

    /**
     * 新增角色
     */
    @PostMapping("/add")
    @ResponseBody
    public ResponseVo addRole(Role role) {
        try {
            int a = roleService.insert(role);
            if (a > 0) {
                return ResultUtil.success("添加角色成功");
            } else {
                return ResultUtil.error("添加角色失败");
            }
        } catch (Exception e) {
            log.error(String.format("RoleController.addRole%s", e));
            throw e;
        }
    }

    /**
     * 删除角色。删除前校验该角色下是否还有用户，存在则拒绝删除；实际为逻辑删除（状态置为失效，非物理删除）。
     */
    @PostMapping("/delete")
    @ResponseBody
    public ResponseVo deleteRole(String roleId) {
        if (roleService.findByRoleId(roleId).size() > 0) {
            return ResultUtil.error("删除失败,该角色下存在用户");
        }
        List<String> roleIdsList = Collections.singletonList(roleId);
        int a = roleService.updateStatusBatch(roleIdsList, CoreConst.STATUS_INVALID);
        if (a > 0) {
            return ResultUtil.success("删除角色成功");
        } else {
            return ResultUtil.error("删除角色失败");
        }
    }

    /**
     * 批量删除角色。校验逻辑与单个删除一致：选中的角色中任意一个存在关联用户即整体拒绝，同样是逻辑删除。
     */
    @PostMapping("/batch/delete")
    @ResponseBody
    public ResponseVo batchDeleteRole(@RequestParam("ids[]") String[] ids) {
        List<String> roleIdsList = Arrays.asList(ids);
        if (CollUtil.isNotEmpty(roleService.findByRoleIds(roleIdsList))) {
            return ResultUtil.error("删除失败,选择的角色下存在用户");
        }
        int a = roleService.updateStatusBatch(roleIdsList, CoreConst.STATUS_INVALID);
        if (a > 0) {
            return ResultUtil.success("删除角色成功");
        } else {
            return ResultUtil.error("删除角色失败");
        }
    }

    /**
     * 编辑角色基本信息
     */
    @PostMapping("/edit")
    @ResponseBody
    public ResponseVo editRole(@ModelAttribute("role") Role role) {
        int a = roleService.updateByRoleId(role);
        if (a > 0) {
            return ResultUtil.success("编辑角色成功");
        } else {
            return ResultUtil.error("编辑角色失败");
        }
    }

    /**
     * 查询"分配权限"弹窗所需数据：返回全部有效权限，并逐项标记该角色是否已拥有该权限（勾选状态），供前端渲染权限树。
     */
    @PostMapping("/assign/permission/list")
    @ResponseBody
    public List<PermissionTreeListVo> assignRole(String roleId) {
        List<PermissionTreeListVo> listVos = new ArrayList<>();
        List<Permission> allPermissions = permissionService.selectAll(CoreConst.STATUS_VALID);
        List<Permission> hasPermissions = roleService.findPermissionsByRoleId(roleId);
        for (Permission permission : allPermissions) {
            PermissionTreeListVo vo = new PermissionTreeListVo();
            vo.setId(permission.getId());
            vo.setPermissionId(permission.getPermissionId());
            vo.setName(permission.getName());
            vo.setParentId(permission.getParentId());
            for (Permission hasPermission : hasPermissions) {
                //有权限则选中
                if (hasPermission.getPermissionId().equals(permission.getPermissionId())) {
                    vo.setChecked(true);
                    break;
                }
            }
            listVos.add(vo);
        }
        return listVos;
    }


    /**
     * 保存角色的权限分配结果。分配完成后会反查该角色下所有用户，并逐个清除他们在 MyShiroRealm 中缓存的
     * 授权信息，使权限变更对这些用户的下一次请求立即生效，无需重新登录。
     */
    @PostMapping("/assign/permission")
    @ResponseBody
    public ResponseVo assignRole(String roleId, String permissionIdStr) {
        List<String> permissionIdsList = new ArrayList<>();
        if (StrUtil.isNotBlank(permissionIdStr)) {
            String[] permissionIds = permissionIdStr.split(",");
            permissionIdsList = Arrays.asList(permissionIds);
        }
        try {
            roleService.addAssignPermission(roleId, permissionIdsList);
            /*重新加载角色下所有用户权限*/
            List<User> userList = roleService.findByRoleId(roleId);
            if (!userList.isEmpty()) {
                List<String> userIds = new ArrayList<>();
                for (User user : userList) {
                    userIds.add(user.getUserId());
                }
                myShiroRealm.clearAuthorizationByUserId(userIds);
            }
            return ResultUtil.success("分配权限成功");
        } catch (Exception e) {
            return ResultUtil.error("分配权限失败");
        }
    }

}
