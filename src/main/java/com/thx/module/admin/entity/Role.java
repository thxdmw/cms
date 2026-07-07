package com.thx.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Data
public class Role implements Serializable {

    private static final long serialVersionUID = -80449433292135181L;

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 角色id
     */
    private String roleId;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 状态：1有效; 0无效
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}