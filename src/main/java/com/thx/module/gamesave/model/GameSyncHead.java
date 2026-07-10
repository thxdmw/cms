package com.thx.module.gamesave.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 每个用户、每个游戏唯一的云端同步 HEAD。 */
@Data
@Accessors(chain = true)
@TableName("game_sync_head")
public class GameSyncHead {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String gameId;
    private String headSnapshotId;
    private Long version;
    private Date updateTime;
}
