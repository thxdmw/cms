package com.thx.module.admin.controller.auth;

import com.thx.common.util.ResultUtil;
import com.thx.module.admin.service.UserService;
import com.thx.module.admin.vo.UserOnlineVo;
import com.thx.module.admin.vo.UserSessionVo;
import com.thx.module.admin.vo.base.PageResultVo;
import com.thx.module.admin.vo.base.ResponseVo;
import lombok.AllArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.Serializable;
import java.util.List;

/**
 * 后台在线用户管理接口，对应前端 admin-app 的在线用户模块：查看当前所有在线会话，并可强制踢出指定用户下线。
 */
@Controller
@RequestMapping("/online/user")
@AllArgsConstructor
public class OnlineUserController {

    private final UserService userService;

    /**
     * 在线用户列表。userService.selectOnlineUsers 一次性取回全部在线会话后，这里再手动做内存分页
     * （而非数据库分页），因为在线用户数据来自 Shiro Session 而非数据库表。
     */
    @PostMapping("/list")
    @ResponseBody
    public PageResultVo onlineUsers(UserOnlineVo user, Integer pageNumber, Integer pageSize) {
        List<UserOnlineVo> userList = userService.selectOnlineUsers(user);
        int endIndex = Math.min(pageNumber * pageSize, userList.size());
        return ResultUtil.table(userList.subList((pageNumber - 1) * pageSize, endIndex), (long) userList.size());
    }

    /**
     * 强制踢出单个在线用户（使其会话失效）。会先校验目标会话是否为当前操作者自己的会话，禁止自己踢自己。
     */
    @PostMapping("/kickout")
    @ResponseBody
    public ResponseVo kickout(String sessionId, String username) {
        try {
            if (SecurityUtils.getSubject().getSession().getId().equals(sessionId)) {
                return ResultUtil.error("不能踢出自己");
            }
            userService.kickout(sessionId, username);
            return ResultUtil.success("踢出用户成功");
        } catch (Exception e) {
            return ResultUtil.error("踢出用户失败");
        }
    }

    /**
     * 批量强制踢出在线用户。选中列表中若包含当前操作者自己的会话，会跳过对自己的踢出操作、仅踢出其余用户，
     * 但最终统一提示"不能踢出自己"（未区分本次是否仍有其他用户被成功踢出）。
     */
    @PostMapping("/batch/kickout")
    @ResponseBody
    public ResponseVo kickout(@RequestBody List<UserSessionVo> sessions) {
        try {
            //要踢出的用户中是否有自己
            boolean hasOwn = false;
            Serializable sessionId = SecurityUtils.getSubject().getSession().getId();
            for (UserSessionVo sessionVo : sessions) {
                if (sessionVo.getSessionId().equals(sessionId)) {
                    hasOwn = true;
                } else {
                    userService.kickout(sessionVo.getSessionId(), sessionVo.getUsername());
                }


            }
            if (hasOwn) {
                return ResultUtil.success("不能踢出自己");
            }
            return ResultUtil.success("踢出用户成功");
        } catch (Exception e) {
            return ResultUtil.error("踢出用户失败");
        }
    }
}
