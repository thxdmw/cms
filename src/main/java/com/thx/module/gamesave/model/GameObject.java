package com.thx.module.gamesave.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 用户级内容寻址对象。二进制实体由 module.file 管理。 */
@Data
@Accessors(chain = true)
@TableName("game_object")
public class GameObject {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String objectId;
    private String userId;
    private String sha256;
    private Long size;
    private String fileId;
    private Long referenceCount;
    private String status;
    private Date createTime;
    private Date updateTime;
}
