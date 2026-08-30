package com.thx.module.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.common.annotation.AnonymousAccess;
import com.thx.common.util.CoreConst;
import com.thx.common.vo.ResponseVo;
import com.thx.module.admin.entity.BizArticle;
import com.thx.module.admin.entity.BizCategory;
import com.thx.module.admin.entity.BizTags;
import com.thx.module.admin.service.BizArticleService;
import com.thx.module.admin.service.BizArticleTagsService;
import com.thx.module.admin.service.BizCategoryService;
import com.thx.module.admin.service.BizTagsService;
import com.thx.module.admin.vo.BizArticleSearchVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 博客助手专用接口，只返回模型完成查询与发布所需的字段。
 * API Key 仍由 AgentApiAuthInterceptor 统一校验，AnonymousAccess 仅跳过后台登录态。
 */
@Slf4j
@RestController
@RequestMapping("/agent/api/blog")
@AllArgsConstructor
public class BlogAssistantApiController {

    private static final int DEFAULT_CATEGORY_ID = 1;

    private final BizArticleService articleService;
    private final BizCategoryService categoryService;
    private final BizTagsService tagsService;
    private final BizArticleTagsService articleTagsService;

    @AnonymousAccess
    @GetMapping("/overview")
    public ResponseVo<BlogOverview> overview(
            @RequestParam(value = "recentLimit", defaultValue = "5") int recentLimit) {
        int limit = Math.max(1, Math.min(recentLimit, 10));
        int articleCount = Math.toIntExact(articleService.count(
                new LambdaQueryWrapper<BizArticle>().eq(BizArticle::getStatus, CoreConst.STATUS_VALID)));
        int categoryCount = Math.toIntExact(categoryService.count(
                new LambdaQueryWrapper<BizCategory>().eq(BizCategory::getStatus, CoreConst.STATUS_VALID)));
        int tagCount = Math.toIntExact(tagsService.count());
        List<ArticleSummary> recentArticles = articleService.recentList(limit).stream()
                .map(this::toSummary)
                .toList();
        return ResponseVo.success(new BlogOverview(articleCount, categoryCount, tagCount, recentArticles));
    }

    @AnonymousAccess
    @GetMapping("/articles/search")
    public ResponseVo<List<ArticleReference>> search(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", defaultValue = "10") int resultLimit) {
        requireText(keyword, "搜索关键词", 100);
        int limit = Math.max(1, Math.min(resultLimit, 20));
        List<ArticleReference> results = articleService.search(keyword.trim()).stream()
                .limit(limit)
                .map(this::toReference)
                .toList();
        return ResponseVo.success(results);
    }

    @AnonymousAccess
    @GetMapping("/articles/{id}")
    public ResponseVo<ArticleDetail> detail(@PathVariable String id) {
        BizArticle article = articleService.selectById(id);
        if (article == null || !CoreConst.STATUS_VALID.equals(article.getStatus())) {
            return ResponseVo.error("文章不存在或已删除");
        }
        return ResponseVo.success(toDetail(article));
    }

    @AnonymousAccess
    @GetMapping("/taxonomy")
    public ResponseVo<Taxonomy> taxonomy() {
        List<TaxonomyItem> categories = categoryService.list(
                        new LambdaQueryWrapper<BizCategory>().eq(BizCategory::getStatus, CoreConst.STATUS_VALID))
                .stream().map(category -> new TaxonomyItem(category.getId(), category.getName())).toList();
        List<TaxonomyItem> tags = tagsService.list().stream()
                .map(tag -> new TaxonomyItem(tag.getId(), tag.getName())).toList();
        return ResponseVo.success(new Taxonomy(categories, tags));
    }

    @AnonymousAccess
    @Transactional
    @PostMapping("/articles")
    public ResponseVo<PublishedArticle> publish(@RequestBody PublishArticleRequest request) {
        validatePublication(request);
        BizArticle article = new BizArticle();
        article.setTitle(request.title().trim());
        article.setContentMd(request.contentMd().trim());
        article.setCategoryId(blankToDefault(request.categoryId(), String.valueOf(DEFAULT_CATEGORY_ID)));
        article.setDescription(trimToNull(request.description()));
        article.setKeywords(trimToNull(request.keywords()));
        article.setCoverImage(trimToNull(request.coverImage()));
        article.setAuthor(blankToDefault(request.author(), "AI Assistant"));
        article.setIsMarkdown(true);
        article.setStatus(CoreConst.STATUS_VALID);
        article.setTop(0);
        article.setRecommended(0);
        article.setSlider(0);
        article.setOriginal(1);
        article.setComment(0);
        article.setUserId("1");

        BizArticle saved = articleService.insertArticle(article);
        List<String> tagIds = request.tagIds() == null ? List.of() : request.tagIds().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .limit(10)
                .toList();
        if (!tagIds.isEmpty()) articleTagsService.insertList(tagIds.toArray(String[]::new), saved.getId());
        log.info("博客助手发布文章成功，articleId={}", saved.getId());
        return ResponseVo.success(new PublishedArticle(saved.getId(), saved.getTitle(), saved.getCreateTime()));
    }

    private ArticleSummary toSummary(BizArticle article) {
        String category = article.getBizCategory() == null ? null : article.getBizCategory().getName();
        List<String> tags = article.getTags() == null ? List.of()
                : article.getTags().stream().map(BizTags::getName).toList();
        return new ArticleSummary(article.getId(), article.getTitle(), category, tags, article.getCreateTime());
    }

    private ArticleReference toReference(BizArticleSearchVo article) {
        return new ArticleReference(article.getId(), article.getTitle());
    }

    private ArticleDetail toDetail(BizArticle article) {
        TaxonomyItem category = article.getBizCategory() == null ? null
                : new TaxonomyItem(article.getBizCategory().getId(), article.getBizCategory().getName());
        List<TaxonomyItem> tags = article.getTags() == null ? List.of()
                : article.getTags().stream().map(tag -> new TaxonomyItem(tag.getId(), tag.getName())).toList();
        return new ArticleDetail(article.getId(), article.getTitle(), article.getContentMd(), article.getDescription(),
                category, tags, article.getAuthor(), article.getCreateTime());
    }

    private void validatePublication(PublishArticleRequest request) {
        if (request == null) throw new IllegalArgumentException("发布内容不能为空");
        requireText(request.title(), "标题", 200);
        requireText(request.contentMd(), "Markdown 正文", 100_000);
        if (request.description() != null && request.description().length() > 500) {
            throw new IllegalArgumentException("摘要不能超过 500 个字符");
        }
    }

    private void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + "不能为空");
        if (value.length() > maxLength) throw new IllegalArgumentException(field + "不能超过 " + maxLength + " 个字符");
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record BlogOverview(int totalArticles, int totalCategories, int totalTags,
                               List<ArticleSummary> recentArticles) { }

    public record ArticleSummary(String id, String title, String category, List<String> tags,
                                 Date publishedAt) { }

    public record ArticleReference(String id, String title) { }

    public record ArticleDetail(String id, String title, String contentMd, String description,
                                TaxonomyItem category, List<TaxonomyItem> tags, String author,
                                Date publishedAt) { }

    public record Taxonomy(List<TaxonomyItem> categories, List<TaxonomyItem> tags) { }

    public record TaxonomyItem(String id, String name) { }

    public record PublishArticleRequest(String title, String contentMd, String categoryId, List<String> tagIds,
                                        String description, String keywords, String coverImage, String author) { }

    public record PublishedArticle(String id, String title, Date createdAt) { }
}
