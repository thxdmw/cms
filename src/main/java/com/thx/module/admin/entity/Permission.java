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
 * 权限/菜单节点，通过 parentId 组织成树形结构，同时承担"目录/菜单/按钮"三种层级（见 type 字段）：
 * 目录和菜单构成后台侧边栏导航，按钮级权限则用于页面内的操作按钮显隐控制（配合 perms 权限标识）。
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