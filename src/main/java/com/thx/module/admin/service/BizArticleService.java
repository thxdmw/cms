package com.thx.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizArticle;
import com.thx.module.admin.vo.ArticleConditionVo;
import com.thx.module.blog.vo.BizArticleSearchVo;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 文章服务，覆盖后台管理的条件查询、增删，以及前台首页所需的轮播/推荐/最新/随机/热门等各类文章列表。
 * sliderList/recommendedList/recentList/randomList/hotList/selectById 均带缓存，insertArticle/deleteBatch 会清空文章缓存。
 */
public interface BizArticleService extends IService<BizArticle> {

    /**
     * 按条件分页查询文章列表，并二次查询补全每篇文章关联的标签集合（tags）。
     *
     * @param page 分页参数，为 null 时查询全部（不分页）
     * @param vo   查询条件：详见 {@code BizArticleMapper#findByCondition}
     * @return 文章列表（含分类、标签、浏览/评论/点赞统计）
     */
    List<BizArticle> findByCondition(IPage<BizArticle> page, ArticleConditionVo vo);

    /**
     * 查询首页轮播文章：slider=true 且已发布的文章，结果做了缓存（固定 key "slider"）。
     *
     * @return 轮播文章列表
     */
    List<BizArticle> sliderList();

    /**
     * 查询站长推荐文章：recommended=true 且已发布的文章，结果做了缓存（固定 key "recommended"）。
     *
     * @param pageSize 返回条数上限
     * @return 推荐文章列表
     */
    List<BizArticle> recommendedList(int pageSize);

    /**
     * 查询最近发布的文章（忽略置顶优先级，按创建时间倒序），结果做了缓存（固定 key "recent"）。
     *
     * @param pageSize 返回条数上限
     * @return 最近文章列表
     */
    List<BizArticle> recentList(int pageSize);

    /**
     * 随机查询已发布文章，结果做了缓存（固定 key "random"，同一缓存周期内多次调用返回相同结果）。
     *
     * @param pageSize 返回条数上限
     * @return 随机文章列表
     */
    List<BizArticle> randomList(int pageSize);

    /**
     * 查询热门文章：已发布文章按浏览量降序排列，结果做了缓存（固定 key "hot"）。
     *
     * @param pageSize 返回条数上限
     * @return 热门文章列表
     */
    List<BizArticle> hotList(int pageSize);

    /**
     * 根据 id 查询文章详情（含分类、标签、浏览/评论/点赞统计），结果按 id 做了缓存。
     *
     * @param id 文章 id
     * @return 文章详情
     */
    BizArticle selectById(String id);

    /**
     * 新增文章，自动补全创建时间、更新时间；保存成功后清空文章缓存。
     *
     * @param bizArticle 待新增文章
     * @return 补全时间戳后的文章对象（含生成的 id）
     */
    BizArticle insertArticle(BizArticle bizArticle);

    /**
     * 批量删除文章，成功后清空文章缓存。
     *
     * @param ids 文章 id 数组
     * @return 影响行数
     */
    int deleteBatch(String[] ids);

    /**
     * 根据分类 id 精确查询该分类下的文章列表（不含分类/标签/统计等关联信息）。
     *
     * @param categoryId 分类 id
     * @return 文章列表
     */
    List<BizArticle> selectByCategoryId(String categoryId);


    /**
     * 全文搜索已发布文章：标题或正文包含关键字即命中。
     *
     * @param keyword 搜索关键字
     * @return 搜索结果精简 VO 列表（含跳转链接）
     */
    List<BizArticleSearchVo> search(String keyword);

    /**
     * 将指定文章批量导出为 Markdown 文件下载：单篇文章直接下载为 .md 文件，
     * 多篇文章打包为 .zip；文章标题中的非法文件名字符会被替换为下划线，
     * 若文章未保存 Markdown 原文（contentMd 为空）则退化为"标题 + HTML 正文"拼接内容。
     * 内部异常会被捕获并转换为 500 响应，不会向上抛出。
     *
     * @param ids      待下载的文章 id 列表
     * @param response 用于直接写出文件流的 HTTP 响应
     */
    void downloadArticles(List<String> ids, HttpServletResponse response);
}
