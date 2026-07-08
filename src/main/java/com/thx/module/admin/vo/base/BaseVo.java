package com.thx.module.admin.vo.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 业务实体基类：统一主键（UUID）和创建/更新时间，admin 模块下 Biz 前缀的业务实体
 * （BizArticle、BizCategory 等）都继承自它，避免每个实体重复声明这三个字段。
 */
@Data
public abstract class BaseVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，插入时由 MyBatis-Plus 自动生成 UUID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}