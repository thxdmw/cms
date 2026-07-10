package com.thx.module.gamesave.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 快照中的单个文件映射，relativePath 使用正斜杠作为统一分隔符。 */
@Data
@Accessors(chain = true)
@TableName("game_snapshot_file")
public class GameSnapshotFile {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String snapshotId;
    private String relativePath;
    private String objectId;
    private Long size;
    private String sha256;
    private Date createTime;
}
