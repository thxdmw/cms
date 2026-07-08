package com.thx.module.agent.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.common.annotation.AnonymousAccess;
import com.thx.common.util.CoreConst;
import com.thx.module.admin.entity.BizArticle;
import com.thx.module.admin.entity.BizCategory;
import com.thx.module.admin.entity.BizTags;
import com.thx.module.admin.service.BizArticleService;
import com.thx.module.admin.service.BizCategoryService;
import com.thx.module.admin.service.BizTagsService;
import com.thx.module.admin.vo.base.ResponseVo;
import com.thx.module.blog.vo.BizArticleInfoVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 供外部 AI Agent / 自动化客户端调用的博客读写接口（路径 /agent/api/blog/**）。
 * <p>
 * 这不是给内部管理后台用的普通接口：请求需要在请求头带上 {@code X-API-Key}，由
 * {@link com.thx.common.interceptor.AgentApiAuthInterceptor} 统一拦截校验（配置项
 * {@code agent.api.key}/{@code agent.api.enabled}），校验逻辑独立于 Shiro 会话体系。
 * 方法上的 {@link com.thx.common.annotation.AnonymousAccess} 只是用来告诉 Shiro"这个方法不需要
 * Shiro 登录会话"，并不代表接口是完全开放匿名的——真正的准入控制在上面这层 API Key 拦截器。
 * <p>
 * 本控制器自己不维护数据层，文章/分类/标签的读写全部复用 admin 模块的
 * {@link BizArticleService}/{@link BizCategoryService}/{@link BizTagsService}。
 */
@Slf4j
@RestController
@RequestMapping("/agent/api/blog/")
@AllArgsConstructor
public class BlogAgentApiController {

    private final BizArticleService bizArticleService;
    private final BizCategoryService bizCategoryService;
    private final BizTagsService bizTagsService;
    private final Environment environment;

    /**
     * 获取所有已发布文章的标题
     *
     * @return 所有文章标题拼接成的字符串，以英文逗号分隔
     */
    @AnonymousAccess
    @GetMapping("getAllBlogTitles")
    public String getAllBlogTitles() {
        LambdaQueryWrapper<BizArticle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BizArticle::getStatus, CoreConst.STATUS_VALID);
        List<BizArticle> list = bizArticleService.list(queryWrapper);
        return list.stream().map(BizArticle::getTitle).collect(Collectors.joining(","));
    }

    /**
     * 根据文章标题获取文章 markdown 内容（标题需完全匹配）
     *
     * @param title 文章标题
     * @return 文章的 markdown 正文；找不到对应标题的文章时返回提示字符串"没有标题对应的文章"
     */
    @AnonymousAccess
    @GetMapping("getBlogContentByTitle")
    public String getBlogContentByTitle(@RequestParam("title") String title) {
        LambdaQueryWrapper<BizArticle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BizArticle::getTitle, title)
                .eq(BizArticle::getStatus, CoreConst.STATUS_VALID);
        List<BizArticle> bizArticle = bizArticleService.list(queryWrapper);
        if (bizArticle.size() > 0) {
            return bizArticle.get(0).getContentMd();
        }
        return "没有标题对应的文章";
    }

    /**
     * 获取所有已发布文章的精简信息（标题 + 可直接访问的完整详情页链接），供 Agent 浏览文章清单时使用。
     * 链接的域名前缀根据当前激活的 Spring profile 拼接：{@code prd} 环境用线上域名，其余环境用本地地址。
     *
     * @return 文章标题 + 跳转地址列表，见 {@link BizArticleInfoVo}
     */
    @AnonymousAccess
    @GetMapping("getAllBlogInfos")
    public ResponseVo<List<BizArticleInfoVo>> getAllBlogInfos() {
        LambdaQueryWrapper<BizArticle> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BizArticle::getStatus, CoreConst.STATUS_VALID);
        List<BizArticle> list = bizArticleService.list(queryWrapper);
        List<BizArticleInfoVo> vos = new ArrayList<>();
        if (CollUtil.isNotEmpty(list)) {
            String activeProfile = environment.getActiveProfiles()[0];
            String baseUrl = "prd".equals(activeProfile) ? "https://cms.thxdxw.cn" : "http://localhost:9090";

            list.forEach(bizArticle -> {
                BizArticleInfoVo vo = BeanUtil.copyProperties(bizArticle, BizArticleInfoVo.class);
                vo.setSkipUrl(baseUrl + "/blog/article/" + bizArticle.getId().toString());
                vos.add(vo);
            });
        }
        return ResponseVo.success(vos);
    }

    /**
     * 获取文章详情（包含完整信息）
     *
     * @param id 文章ID
     * @return 文章详情
     */
    @AnonymousAccess
    @GetMapping("getBlogDetailById")
    public ResponseVo<BizArticle> getBlogDetailById(@RequestParam("id") String id) {
        BizArticle article = bizArticleService.selectById(id);
        if (article != null && CoreConst.STATUS_VALID.equals(article.getStatus())) {
            return ResponseVo.success(article);
        }
        return ResponseVo.error("文章不存在或已删除");
    }

    /**
     * 根据分类ID获取文章列表
     *
     * @param categoryId 分类ID
     * @return 文章列表
     */
    @AnonymousAccess
    @GetMapping("getBlogsByCategoryId")
    public ResponseVo<List<BizArticle>> getBlogsByCategoryId(@RequestParam("categoryId") String categoryId) {
        List<BizArticle> articles = bizArticleService.selectByCategoryId(categoryId);
        return ResponseVo.success(articles);
    }

    /**
     * 获取所有分类
     *
     * @return 分类列表
     */
    @AnonymousAccess
    @GetMapping("getAllCategories")
    public ResponseVo<List<BizCategory>> getAllCategories() {
        LambdaQueryWrapper<BizCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BizCategory::getStatus, CoreConst.STATUS_VALID);
        List<BizCategory> categories = bizCategoryService.list(queryWrapper);
        return ResponseVo.success(categories);
    }

    /**
     * 获取所有标签
     *
     * @return 标签列表
     */
    @AnonymousAccess
    @GetMapping("getAllTags")
    public ResponseVo<List<BizTags>> getAllTags() {
        List<BizTags> tags = bizTagsService.list();
        return ResponseVo.success(tags);
    }

    /**
     * 搜索文章
     *
     * @param keyword 搜索关键词
     * @return 搜索结果
     */
    @AnonymousAccess
    @GetMapping("searchBlogs")
    public ResponseVo<List<?>> searchBlogs(@RequestParam("keyword") String keyword) {
        try {
            List<?> results = bizArticleService.search(keyword);
            return ResponseVo.success(results);
        } catch (Exception e) {
            log.error("搜索文章失败", e);
            return ResponseVo.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取最新文章
     *
     * @param pageSize 每页数量，默认10
     * @return 最新文章列表
     */
    @AnonymousAccess
    @GetMapping("getRecentBlogs")
    public ResponseVo<List<BizArticle>> getRecentBlogs(
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        List<BizArticle> articles = bizArticleService.recentList(pageSize);
        return ResponseVo.success(articles);
    }

    /**
     * 获取热门文章
     *
     * @param pageSize 每页数量，默认10
     * @return 热门文章列表
     */
    @AnonymousAccess
    @GetMapping("getHotBlogs")
    public ResponseVo<List<BizArticle>> getHotBlogs(
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        List<BizArticle> articles = bizArticleService.hotList(pageSize);
        return ResponseVo.success(articles);
    }

    /**
     * 获取推荐文章
     *
     * @param pageSize 每页数量，默认10
     * @return 推荐文章列表
     */
    @AnonymousAccess
    @GetMapping("getRecommendedBlogs")
    public ResponseVo<List<BizArticle>> getRecommendedBlogs(
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        List<BizArticle> articles = bizArticleService.recommendedList(pageSize);
        return ResponseVo.success(articles);
    }

    /**
     * 发布新文章
     *
     * @param title       文章标题
     * @param contentMd   Markdown格式内容
     * @param content     HTML格式内容（可选）
     * @param categoryId  分类ID
     * @param tagIds      标签ID数组（可选）
     * @param description 文章描述
     * @param keywords    关键词
     * @param coverImage  封面图片URL
     * @param isMarkdown  是否为Markdown格式，默认true
     * @param author      作者名
     * @return 发布的文章
     */
    @AnonymousAccess
    @PostMapping("publishBlog")
    public ResponseVo<BizArticle> publishBlog(
            @RequestParam("title") String title,
            @RequestParam("contentMd") String contentMd,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "tagIds", required = false) String[] tagIds,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "coverImage", required = false) String coverImage,
            @RequestParam(value = "isMarkdown", defaultValue = "true") Boolean isMarkdown,
            @RequestParam(value = "author", defaultValue = "AI Agent") String author) {
        try {
            // 创建文章对象
            BizArticle article = new BizArticle();
            article.setTitle(title);
            article.setContentMd(contentMd);
            article.setContent(content);
            article.setCategoryId(categoryId != null ? categoryId : "1"); // 默认分类ID为1
            article.setDescription(description);
            article.setKeywords(keywords);
            article.setCoverImage(coverImage);
            article.setIsMarkdown(isMarkdown);
            article.setAuthor(author);
            article.setStatus(CoreConst.STATUS_VALID);
            article.setTop(0);
            article.setRecommended(0);
            article.setSlider(0);
            article.setOriginal(1); // 默认为原创
            article.setComment(0);
            article.setUserId("1"); // 默认用户ID

            // 插入文章
            BizArticle savedArticle = bizArticleService.insertArticle(article);

            // 如果有标签，关联标签
            if (tagIds != null && tagIds.length > 0) {
                // 注意：这里需要注入BizArticleTagsService来处理标签关联
                // 暂时省略，后续可以扩展
            }

            log.info("Agent发布文章成功: {}", title);
            return ResponseVo.success(savedArticle);
        } catch (Exception e) {
            log.error("发布文章失败", e);
            return ResponseVo.error("发布文章失败: " + e.getMessage());
        }
    }

    /**
     * 更新文章
     *
     * @param id          文章ID
     * @param title       文章标题
     * @param contentMd   Markdown格式内容
     * @param content     HTML格式内容（可选）
     * @param categoryId  分类ID
     * @param description 文章描述
     * @param keywords    关键词
     * @param coverImage  封面图片URL
     * @return 更新后的文章
     */
    @AnonymousAccess
    @PostMapping("updateBlog")
    public ResponseVo<BizArticle> updateBlog(
            @RequestParam("id") String id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "contentMd", required = false) String contentMd,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "coverImage", required = false) String coverImage) {
        try {
            BizArticle article = bizArticleService.selectById(id);
            if (article == null) {
                return ResponseVo.error("文章不存在");
            }

            // 更新字段
            if (title != null) article.setTitle(title);
            if (contentMd != null) article.setContentMd(contentMd);
            if (content != null) article.setContent(content);
            if (categoryId != null) article.setCategoryId(categoryId);
            if (description != null) article.setDescription(description);
            if (keywords != null) article.setKeywords(keywords);
            if (coverImage != null) article.setCoverImage(coverImage);

            article.setUpdateTime(new Date());
            bizArticleService.updateById(article);

            log.info("Agent更新文章成功: {}", id);
            return ResponseVo.success(article);
        } catch (Exception e) {
            log.error("更新文章失败", e);
            return ResponseVo.error("更新文章失败: " + e.getMessage());
        }
    }

    /**
     * 删除文章
     *
     * @param id 文章ID
     * @return 操作结果
     */
    @AnonymousAccess
    @PostMapping("deleteBlog")
    public ResponseVo<String> deleteBlog(@RequestParam("id") String id) {
        try {
            BizArticle article = bizArticleService.selectById(id);
            if (article == null) {
                return ResponseVo.error("文章不存在");
            }

            // 软删除：将状态设置为无效
            article.setStatus(CoreConst.STATUS_INVALID);
            article.setUpdateTime(new Date());
            bizArticleService.updateById(article);

            log.info("Agent删除文章成功: {}", id);
            return ResponseVo.success("文章删除成功");
        } catch (Exception e) {
            log.error("删除文章失败", e);
            return ResponseVo.error("删除文章失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统统计信息
     *
     * @return 统计信息
     */
    @AnonymousAccess
    @GetMapping("getSystemStats")
    public ResponseVo<SystemStatsVo> getSystemStats() {
        try {
            SystemStatsVo stats = new SystemStatsVo();

            // 文章总数
            long totalArticles = bizArticleService.count(
                    new LambdaQueryWrapper<BizArticle>().eq(BizArticle::getStatus, CoreConst.STATUS_VALID));
            stats.setTotalArticles((int) totalArticles);

            // 分类总数
            long totalCategories = bizCategoryService.count(
                    new LambdaQueryWrapper<BizCategory>().eq(BizCategory::getStatus, CoreConst.STATUS_VALID));
            stats.setTotalCategories((int) totalCategories);

            // 标签总数
            long totalTags = bizTagsService.count();
            stats.setTotalTags((int) totalTags);

            // 最新文章
            List<BizArticle> recentArticles = bizArticleService.recentList(5);
            stats.setRecentArticles(recentArticles);

            return ResponseVo.success(stats);
        } catch (Exception e) {
            log.error("获取系统统计信息失败", e);
            return ResponseVo.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 系统统计信息VO
     */
    @Data
    public static class SystemStatsVo {
        private Integer totalArticles;
        private Integer totalCategories;
        private Integer totalTags;
        private List<BizArticle> recentArticles;
    }
}
