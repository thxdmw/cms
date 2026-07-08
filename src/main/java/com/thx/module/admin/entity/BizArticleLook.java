package com.thx.module.admin.entity;

import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 文章浏览记录：每条记录代表一次访问，BizArticle.lookCount 就是按 articleId 统计这张表的行数得到的。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizArticleLook extends BaseVo {
    private static final long serialVersionUID = 1052723347580827581L;

    /** 被浏览的文章 id */
    private String articleId;
    /** 浏览者用户 id（未登录访客可能为空） */
    private String userId;
    /** 浏览者 IP，未登录访客用来做粗粒度去重/统计 */
    private String userIp;
    /** 浏览时间 */
    private Date lookTime;

}
