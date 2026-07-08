package com.thx.module.blog.vo;

import lombok.Data;

/**
 * 文章的精简展示信息：标题 + 可直接访问的完整跳转地址。
 * 目前只被 {@code com.thx.module.agent.controller.BlogAgentApiController#getAllBlogInfos()} 使用，
 * 用于给外部 Agent 返回一份轻量的文章清单（不含正文），避免把完整的 {@code BizArticle} 都吐给调用方。
 */
@Data
public class BizArticleInfoVo {
    /** 文章标题 */
    private String title;
    /** 文章详情页的完整跳转地址（含域名，如 https://cms.thxdxw.cn/blog/article/1） */
    private String skipUrl;
}
