package com.thx.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Data
@Accessors(chain = true)
public class Permission implements Serializable {

    private static final long serialVersionUID = -4317225965160245362L;

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 权限id
     */
    private String permissionId;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;

    /**
     * 权限访问路径
     */
    private String url;

    /**
     * 权限标识
     */
    private String perms;

    /**
     * 父级权限id（引用 permission.id，顶层用字符串 "0"）
     */
    private String parentId;

    /**
     * 类型   0：目录   1：菜单   2：按钮
     */
    private Integer type;

    /**
     * 排序
     */
    private Integer orderNum;

    /**
     * 图标
     */
    private String icon;
    /**
     * 状态：1有效; 0无效
     */
    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableField(exist = false)
    private List<Permission> children;

}