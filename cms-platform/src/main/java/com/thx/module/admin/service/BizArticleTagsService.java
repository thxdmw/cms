package com.thx.module.admin.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizArticleTags;

/**
 * 文章-标签关联服务，维护 biz_article_tags 中间表，供文章保存/编辑时同步标签关联使用。
 */
public interface BizArticleTagsService extends IService<BizArticleTags> {


    /**
     * 删除指定文章的全部标签关联记录，通常在重新绑定标签前先清空旧关联。
     *
     * @param articleId 文章 id
     * @return 删除的记录数
     */
    int removeByArticleId(String articleId);

    /**
     * 为指定文章批量插入标签关联记录（逐条 insert，非批量 SQL）。
     *
     * @param tagIds    标签 id 数组
     * @param articleId 文章 id
     */
    void insertList(String[] tagIds, String articleId);
}
