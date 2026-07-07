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
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Service
@AllArgsConstructor
public class BizArticleTagsServiceImpl extends ServiceImpl<BizArticleTagsMapper, BizArticleTags> implements BizArticleTagsService {

    private final BizArticleTagsMapper bizArticleTagsMapper;

    @Override
    public int removeByArticleId(String articleId) {
        return bizArticleTagsMapper.delete(Wrappers.<BizArticleTags>lambdaQuery().eq(BizArticleTags::getArticleId, articleId));
    }

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
