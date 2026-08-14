package com.thx.module.admin.service;

import java.util.Map;

/**
 * 站点概览信息服务，供前台首页等场景展示文章数、标签数、分类数、评论数等统计数据。
 */
@FunctionalInterface
public interface BizSiteInfoService {

    /**
     * 统计站点概览信息：已发布文章数、标签总数、启用分类数、已发布评论数。
     *
     * @return 包含 articleCount、tagCount、categoryCount、commentCount 四个键的统计结果
     */
    Map<String, Object> getSiteInfo();

}
