package com.thx.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.module.admin.entity.BizArticle;
import com.thx.module.admin.vo.ArticleConditionVo;
import com.thx.module.file.model.FileAsset;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 文章 Mapper，承担文章列表查询、详情查询、热门排行、站点概览统计等复合查询，
 * 多数方法会关联分类、标签、浏览/评论/点赞统计等信息。
 */
public interface BizArticleMapper extends BaseMapper<BizArticle> {

    /**
     * 按条件分页查询文章列表，关联查出所属分类（bizCategory）信息、浏览量（lookCount）、
     * 评论数（commentCount，仅统计 status=1 的评论）、点赞数（loveCount，仅统计 bizType=1 且 status=1 的点赞记录）。
     * 注意：该查询未 select 标签相关列，返回对象的 tags 集合不会被填充，如需标签详情请使用 {@link #getById(String)}。
     *
     * @param page 分页参数
     * @param vo   查询条件：tagId 不为空时按"文章包含该标签"过滤；categoryId/top/status/recommended/slider 精确匹配；
     *             keywords 对 title/content/description/keywords 做模糊匹配（任一命中即可）；
     *             random 不为空时随机排序，否则按 sliderFlag（轮播图优先）、recentFlag（为 true 时不按置顶优先）、create_time 倒序排列
     * @return 分页结果
     */
    List<BizArticle> findByCondition(@Param("page") IPage<BizArticle> page, @Param("vo") ArticleConditionVo vo);

    /**
     * 根据文章 id 列表查询这些文章，并关联查出每篇文章的标签（INNER JOIN biz_article_tags、biz_tags）。
     * 一篇文章可能关联多个标签，结果按"文章-标签"逐条展开。
     *
     * @param list 文章 id 列表
     * @return 文章（含标签信息）列表
     */
    List<BizArticle> listTagsByArticleId(List<String> list);

    /**
     * 分页查询热门文章：仅包含已发布（status=1）的文章，按浏览量（lookCount）降序排列。
     *
     * @param page 分页参数
     * @return 热门文章列表
     */
    List<BizArticle> hotList(@Param("page") IPage<BizArticle> page);

    /**
     * 根据 id 查询文章详情，关联查出所属分类、全部标签、浏览量、评论数（status=1）、点赞数（bizType=1 且 status=1）。
     *
     * @param id 文章 id
     * @return 文章详情，包含分类与标签信息
     */
    BizArticle getById(String id);

    /**
     * 根据 id 数组批量删除文章。
     *
     * @param ids 文章 id 数组
     * @return 影响行数
     */
    int deleteBatch(String[] ids);

    /**
     * 统计站点概览信息：已发布文章数（status=1）、标签总数、启用分类数（status=1）、已发布评论数（status=1）。
     *
     * @return 包含 articleCount、tagCount、categoryCount、commentCount 四个键的统计结果
     */
    Map<String, Object> getSiteInfo();

    /**
     * 查找未被任何文章引用的文章图片（孤立文件）
     * 通过检查 object_key 是否出现在文章的 content、cover_image、slider_img 中
     */
    @Select("SELECT fa.* FROM file_asset fa " +
            "WHERE fa.app_id = 'cms' " +
            "AND fa.namespace = 'article-image' " +
            "AND fa.status = 'ACTIVE' " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM biz_article ba " +
            "    WHERE ba.content LIKE CONCAT('%', fa.object_key, '%') " +
            "       OR ba.cover_image LIKE CONCAT('%', fa.object_key, '%') " +
            "       OR ba.slider_img LIKE CONCAT('%', fa.object_key, '%')" +
            ") " +
            "ORDER BY fa.create_time DESC")
    List<FileAsset> findOrphanArticleImages();
}
