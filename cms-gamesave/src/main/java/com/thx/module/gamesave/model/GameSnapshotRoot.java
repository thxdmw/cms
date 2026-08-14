package com.thx.module.gamesave.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 不可变快照中的存档根目录元数据，不参与服务端文件系统解析。 */
@Data
@Accessors(chain = true)
@TableName("game_snapshot_root")
public class GameSnapshotRoot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String snapshotId;
    private String rootId;
    private String rootType;
    private String pathTemplate;
    private String source;
    private Integer confidence;
    private String includePatternsJson;
    private String excludePatternsJson;
    private Date createTime;
}
