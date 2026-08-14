package com.thx.module.gamesave.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 删除游戏时创建的可重入后台清理任务。 */
@Data
@Accessors(chain = true)
@TableName("game_cleanup_task")
public class GameCleanupTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String userId;
    private String gameId;
    private String status;
    @TableField("`cursor`")
    private Long cursor;
    private Integer retryCount;
    private String lastError;
    private String workerId;
    private Date leaseUntil;
    private Date lastHeartbeatTime;
    private Date createTime;
    private Date updateTime;
}
