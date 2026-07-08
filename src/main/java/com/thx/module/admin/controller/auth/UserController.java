package com.thx.module.admin.controller.auth;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.thx.common.shiro.MyShiroRealm;
import com.thx.common.util.*;
import com.thx.module.admin.entity.Role;
import com.thx.module.admin.entity.User;
import com.thx.module.admin.service.RoleService;
import com.thx.module.admin.service.UserService;
import com.thx.module.admin.vo.ChangePasswordVo;
import com.thx.module.admin.vo.base.PageResultVo;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 后台用户管理接口，对应前端 admin-app 的用户管理模块：用户列表、增删改、角色分配、修改密码。
 */
@Controller
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {

    private final MyShiroRealm shiroRealm;
    private final UserService userService;
    private final RoleService roleService;


    /**
     * 用户列表数据
     */
    @PostMapping("/list")
    @ResponseBody
    public PageResultVo loadUsers(User user, Integer pageNumber, Integer pageSize) {
        IPage<User> userPage = userService.selectUsers(user, pageNumber, pageSize);
        return ResultUtil.table(userPage.getRecords(), userPage.getTotal());
    }

    /**
     * 新增用户。薄层转发：入参校验、密码加密、唯一性校验等业务逻辑全部在 UserService#registerNewUser
     * 内完成，本方法不做任何额外处理。
     */
    @PostMapping("/add")
    @ResponseBody
    public ResponseVo add(User userForm, String confirmPassword) {
        return userService.registerNewUser(userForm, confirmPassword);
    }

    /**
     * 编辑用户基本信息
     */
    @PostMapping("/edit")
    @ResponseBody
    public ResponseVo editUser(User userForm) {
        int a = userService.updateByUserId(userForm);
        if (a > 0) {
            return ResultUtil.success("编辑用户成功！");
        } else {
            return ResultUtil.error("编辑用户失败");
        }
    }

    /**
     * 删除用户。实为逻辑删除：将用户状态置为 CoreConst.STATUS_INVALID，并非物理删除，内部复用批量接口。
     */
    @PostMapping("/delete")
    @ResponseBody
    public ResponseVo deleteUser(String userId) {
        List<String> userIdsList = Arrays.asList(userId);
        boolean a = userService.updateStatusBatch(userIdsList, CoreConst.STATUS_INVALID);
        if (a) {
            return ResultUtil.success("删除用户成功");
        } else {
            return ResultUtil.error("删除用户失败");
        }
    }

    /**
     * 批量删除用户，同样是将状态置为失效的逻辑删除。
     */
    @PostMapping("/batch/delete")
    @ResponseBody
    public ResponseVo batchDeleteUser(@RequestParam("ids[]") String[] ids) {
        List<String> userIdsList = Arrays.asList(ids);
        boolean a = userService.updateStatusBatch(userIdsList, CoreConst.STATUS_INVALID);
        if (a) {
            return ResultUtil.success("删除用户成功");
        } else {
            return ResultUtil.error("删除用户失败");
        }
    }

    /**
     * 查询"分配角色"弹窗所需数据：全部启用状态的角色列表，以及该用户当前已拥有的角色 ID 集合（供前端回显勾选）。
     */
    @PostMapping("/assign/role/list")
    @ResponseBody
    public Map<String, Object> assignRoleList(String userId) {
        List<Role> roleList = roleService.list(Wrappers.<Role>lambdaQuery().eq(Role::getStatus, 1));
        Set<String> hasRoles = roleService.findRoleByUserId(userId);
        Map<String, Object> jsonMap = new HashMap<>(2);
        jsonMap.put("rows", roleList);
        jsonMap.put("hasRoles", hasRoles);
        return jsonMap;
    }

    /**
     * 保存用户的角色分配结果。分配完成后立即清除该用户在 MyShiroRealm 中缓存的授权信息，
     * 使新的角色/权限组合无需重新登录即可对该用户下一次请求生效。
     */
    @PostMapping("/assign/role")
    @ResponseBody
    public ResponseVo assignRole(String userId, String roleIdStr) {
        ResponseVo responseVo;
        String[] roleIds = roleIdStr.split(",");
        List<String> roleIdsList = Arrays.asList(roleIds);
        try {
            // 给用户分配角色
            userService.addAssignRole(userId, roleIdsList);
            // 重置用户权限
            shiroRealm.clearAuthorizationByUserId(Collections.singletonList(userId));
            responseVo = ResultUtil.success("分配角色成功");
        } catch (Exception e) {
            responseVo = ResultUtil.error("分配角色失败");
        }
        return responseVo;
    }

    /**
     * 修改当前登录用户密码。薄层转发：新旧密码校验、一致性校验、清除 Shiro 认证缓存等逻辑
     * 全部在 UserService#changePassword 内完成，本方法不做任何额外处理。
     */
    @RequestMapping(value = "/changePassword", method = RequestMethod.POST)
    @ResponseBody
    public ResponseVo changePassword(ChangePasswordVo changePasswordVo) {
        return userService.changePassword(changePasswordVo);
    }

}
