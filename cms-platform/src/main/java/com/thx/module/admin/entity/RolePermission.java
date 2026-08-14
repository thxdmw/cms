package com.thx.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色与权限的多对多关联表。
 */
@Data
public class RolePermission implements Serializable {

    private static final long serialVersionUID = -902800328539403089L;

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 角色id
     */
    private String roleId;

    /**
     * 权限id
     */
    private String permissionId;

}