package com.thx.module.agent.controller;

import com.thx.common.util.CoreConst;
import com.thx.common.vo.ResponseVo;
import com.thx.module.admin.entity.BizArticle;
import com.thx.module.admin.service.BizArticleService;
import com.thx.module.admin.service.BizArticleTagsService;
import com.thx.module.admin.service.BizCategoryService;
import com.thx.module.admin.service.BizTagsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlogAssistantApiControllerTest {

    @Test
    void 发布只写入允许字段并保存去重后的标签关联() {
        BizArticleService articleService = mock(BizArticleService.class);
        BizCategoryService categoryService = mock(BizCategoryService.class);
        BizTagsService tagsService = mock(BizTagsService.class);
        BizArticleTagsService articleTagsService = mock(BizArticleTagsService.class);
        when(articleService.insertArticle(any())).thenAnswer(invocation -> {
            BizArticle article = invocation.getArgument(0);
            article.setId("article-1");
            return article;
        });
        BlogAssistantApiController controller = new BlogAssistantApiController(
                articleService, categoryService, tagsService, articleTagsService);

        ResponseVo<BlogAssistantApiController.PublishedArticle> response = controller.publish(
                new BlogAssistantApiController.PublishArticleRequest(
                        " 测试文章 ", " # 正文 ", "category-1", List.of("tag-1", "tag-1", "tag-2"),
                        "摘要", "Java", null, null));

        ArgumentCaptor<BizArticle> articleCaptor = ArgumentCaptor.forClass(BizArticle.class);
        verify(articleService).insertArticle(articleCaptor.capture());
        BizArticle article = articleCaptor.getValue();
        assertThat(article.getTitle()).isEqualTo("测试文章");
        assertThat(article.getContentMd()).isEqualTo("# 正文");
        assertThat(article.getStatus()).isEqualTo(CoreConst.STATUS_VALID);
        assertThat(article.getAuthor()).isEqualTo("AI Assistant");
        verify(articleTagsService).insertList(aryEq(new String[]{"tag-1", "tag-2"}), eq("article-1"));
        assertThat(response.getData().id()).isEqualTo("article-1");
    }
}
