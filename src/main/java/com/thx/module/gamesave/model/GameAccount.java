package com.thx.module.gamesave.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * GameSave 独立用户账号。
 * 与 CMS 后台管理员账号完全隔离，避免把客户端用户接入 Shiro/RBAC 管理模型。
 */
@Data
@Accessors(chain = true)
@TableName("game_account")
public class GameAccount {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String username;
    private String passwordHash;
    private Long quotaBytes;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
