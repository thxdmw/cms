package com.thx.module.admin.vo;

import lombok.Data;

/**
 * 评论查询/提交条件封装，字段含义与 {@link com.thx.module.admin.entity.BizComment} 对应字段一致。
 */
@Data
public class CommentConditionVo {
    /** 评论者用户 id */
    private String userId;
    /** 所属文章 id */
    private String sid;
    /** 被回复的父评论 id */
    private String pid;
    /** 评论者 QQ 号 */
    private String qq;
    /** 评论者邮箱 */
    private String email;
    /** 评论者个人主页地址 */
    private String url;
    /** 审核状态：1 已通过 0 待审核 */
    private Integer status;

}

