package com.thx.module.blog.vo;

import com.thx.module.admin.vo.base.BaseConditionVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 文章搜索结果条目：{@code BizArticleService.search(keyword)} 按标题/内容模糊匹配文章后，
 * 把每条命中的 {@code BizArticle} 精简成 id + 标题 + 跳转地址，供前台搜索框（新 Vue SPA 和旧的
 * static/js/search-box.js）直接渲染成可点击的搜索结果列表。
 * <p>
 * 继承 {@link BaseConditionVo} 只是复用其分页字段（pageNumber/pageSize），当前搜索接口暂未分页。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class BizArticleSearchVo extends BaseConditionVo {
    /** 文章 id */
    private String id;
    /** 文章标题 */
    private String title;
    /** 文章详情页的相对跳转路径，形如 "blog/article/1" */
    private String skipUrl;
}

