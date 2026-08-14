package com.thx.module.gamesave.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * GameSave 客户端设备。
 * tokenHash 只保存设备 Token 的 SHA-256，禁止把明文 Token 落库或写入日志。
 */
@Data
@Accessors(chain = true)
@TableName("game_device")
public class GameDevice {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private String userId;
    private String deviceName;
    private String tokenHash;
    private Date tokenExpireTime;
    private Date lastSeenTime;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
