package com.thx.module.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.module.admin.mapper.BizArticleTagsMapper;
import com.thx.module.admin.entity.BizArticleTags;
import com.thx.module.admin.service.BizArticleTagsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 文章-标签关联服务实现，维护 biz_article_tags 中间表。
 */
@Service
@AllArgsConstructor
public class BizArticleTagsServiceImpl extends ServiceImpl<BizArticleTagsMapper, BizArticleTags> implements BizArticleTagsService {

    private final BizArticleTagsMapper bizArticleTagsMapper;

    /** 删除指定文章的全部标签关联记录。 */
    @Override
    public int removeByArticleId(String articleId) {
        return bizArticleTagsMapper.delete(Wrappers.<BizArticleTags>lambdaQuery().eq(BizArticleTags::getArticleId, articleId));
    }

    /** 为指定文章逐条插入标签关联记录，自动补全创建/更新时间。 */
    @Override
    public void insertList(String[] tagIds, String articleId) {
        Date date = new Date();
        for (String tagId : tagIds) {
            BizArticleTags articleTags = new BizArticleTags();
            articleTags.setTagId(tagId);
            articleTags.setArticleId(articleId);
            articleTags.setCreateTime(date);
            articleTags.setUpdateTime(date);
            bizArticleTagsMapper.insert(articleTags);
        }
    }
}
