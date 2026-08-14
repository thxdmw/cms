package com.thx.module.admin.vo;

import lombok.Data;

/**
 * 批量踢出在线用户接口的请求体（见 OnlineUserController#kickout），
 * 只携带定位一个 Session 所需的最小信息。
 */
@Data
public class UserSessionVo {

    /** 要踢出的会话 id */
    private String sessionId;
    /** 该会话所属用户名 */
    private String username;

}
