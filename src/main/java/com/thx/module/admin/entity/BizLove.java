package com.thx.module.admin.entity;


import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通用点赞记录：bizType 区分点赞的是文章还是评论等不同业务对象，bizId 是被点赞对象的 id，
 * 这样一张表就能承载多种业务的点赞需求，不用每种业务各建一张点赞表。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizLove extends BaseVo {
    private static final long serialVersionUID = 6825108677279625433L;

    /** 被点赞对象的 id（如文章 id、评论 id） */
    private String bizId;
    /** 被点赞对象的类型（用于区分 bizId 指向的是文章、评论等哪种业务对象） */
    private Integer bizType;
    /** 点赞用户 id（游客点赞时为空） */
    private String userId;
    /** 点赞者 IP，未登录访客用来做粗粒度去重 */
    private String userIp;
    /** 状态：1 有效 0 已取消 */
    private Integer status;

}
