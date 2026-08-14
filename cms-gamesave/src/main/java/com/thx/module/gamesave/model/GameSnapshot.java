package com.thx.module.gamesave.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 不可变游戏存档快照；创建后禁止覆盖 Manifest。 */
@Data
@Accessors(chain = true)
@TableName("game_snapshot")
public class GameSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String snapshotId;
    private String userId;
    private String gameId;
    private String deviceId;
    private String parentSnapshotId;
    private String triggerType;
    private String description;
    private Integer fileCount;
    private Long logicalSize;
    private Integer changedFileCount;
    private String status;
    private Date createTime;
}
