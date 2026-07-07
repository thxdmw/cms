package com.thx.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Data
public class UserRole implements Serializable {

    private static final long serialVersionUID = 2455220013366482894L;

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 角色id
     */
    private String roleId;

}