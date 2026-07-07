package com.thx.module.admin.controller;

import com.thx.common.shiro.ShiroService;
import com.thx.common.util.CoreConst;
import com.thx.common.util.ResultUtil;
import com.thx.module.admin.entity.Permission;
import com.thx.module.admin.service.PermissionService;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 后台权限配置管理
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Slf4j
@Controller
@RequestMapping("/permission")
@AllArgsConstructor
public class PermissionController {

    /**
     * 1:全部资源，2：菜单资源
     */
    private static final String[] MENU_FLAG = {"1", "2"};

    private final PermissionService permissionService;
    private final ShiroService shiroService;


    /*权限列表数据*/
    @PostMapping("/list")
    @ResponseBody
    public List<Permission> loadPermissions(String flag) {
        List<Permission> permissionListList = new ArrayList<Permission>();
        if (StrUtil.isBlank(flag) || MENU_FLAG[0].equals(flag)) {
            permissionListList = permissionService.selectAll(CoreConst.STATUS_VALID);
        } else if (MENU_FLAG[1].equals(flag)) {
            permissionListList = permissionService.selectAllMenuName(CoreConst.STATUS_VALID);
        }
        return permissionListList;
    }

    /*添加权限*/
    @ResponseBody
    @PostMapping("/add")
    public ResponseVo addPermission(Permission permission) {
        try {
            int a = permissionService.insert(permission);
            if (a > 0) {
                shiroService.updatePermission();
                return ResultUtil.success("添加权限成功");
            } else {
                return ResultUtil.error("添加权限失败");
            }
        } catch (Exception e) {
            log.error(String.format("PermissionController.addPermission%s", e));
            throw e;
        }
    }

    /*删除权限*/
    @ResponseBody
    @PostMapping("/delete")
    public ResponseVo deletePermission(String permissionId) {
        try {
            int subPermsByPermissionIdCount = permissionService.selectSubPermsByPermissionId(permissionId);
            if (subPermsByPermissionIdCount > 0) {
                return ResultUtil.error("改资源存在下级资源，无法删除！");
            }
            int a = permissionService.updateStatus(permissionId, CoreConst.STATUS_INVALID);
            if (a > 0) {
                shiroService.updatePermission();
                return ResultUtil.success("删除权限成功");
            } else {
                return ResultUtil.error("删除权限失败");
            }
        } catch (Exception e) {
            log.error(String.format("PermissionController.deletePermission%s", e));
            throw e;
        }
    }

    /*编辑权限*/
    @ResponseBody
    @PostMapping("/edit")
    public ResponseVo editPermission(@ModelAttribute("permission") Permission permission) {
        int a = permissionService.updateByPermissionId(permission);
        if (a > 0) {
            shiroService.updatePermission();
            return ResultUtil.success("编辑权限成功");
        } else {
            return ResultUtil.error("编辑权限失败");
        }
    }

}
