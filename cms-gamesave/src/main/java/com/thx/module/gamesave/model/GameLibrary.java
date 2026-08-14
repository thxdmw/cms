package com.thx.module.gamesave.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 用户云端逻辑游戏，不保存某台电脑上的 EXE 或存档绝对路径。 */
@Data
@Accessors(chain = true)
@TableName("game_library")
public class GameLibrary {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String gameId;
    private String userId;
    private String gameKey;
    private String name;
    private String provider;
    private String providerGameId;
    private String coverFileId;
    private Integer retentionEnabled;
    private Integer retentionCount;
    private Integer retentionDays;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
