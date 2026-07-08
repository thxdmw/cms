package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thx.module.admin.entity.BizArticleTags;

/**
 * 文章-标签关联表 Mapper，维护文章（BizArticle）与标签（BizTags）之间的多对多关系。
 * 未定义自定义查询方法，增删改查直接使用 MyBatis-Plus 提供的通用 BaseMapper 能力。
 */
public interface BizArticleTagsMapper extends BaseMapper<BizArticleTags> {

}
