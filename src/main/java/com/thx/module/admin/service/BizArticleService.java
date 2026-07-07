package com.thx.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizArticle;
import com.thx.module.admin.vo.ArticleConditionVo;
import com.thx.module.blog.vo.BizArticleSearchVo;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public interface BizArticleService extends IService<BizArticle> {

    /**
     * 分页查询
     *
     * @param vo
     * @return
     */
    List<BizArticle> findByCondition(IPage<BizArticle> page, ArticleConditionVo vo);

    /**
     * 首页轮播
     *
     * @return
     */
    List<BizArticle> sliderList();

    /**
     * 站长推荐
     *
     * @param pageSize
     * @return
     */
    List<BizArticle> recommendedList(int pageSize);

    /**
     * 最近文章
     *
     * @param pageSize
     * @return
     */

    List<BizArticle> recentList(int pageSize);

    /**
     * 随机文章
     *
     * @param pageSize
     * @return
     */
    List<BizArticle> randomList(int pageSize);

    /**
     * 热门文章
     *
     * @param pageSize
     * @return
     */
    List<BizArticle> hotList(int pageSize);

    /**
     * 根据id获取文章
     *
     * @param id
     * @return
     */
    BizArticle selectById(String id);

    /**
     * 插入
     *
     * @return
     */
    BizArticle insertArticle(BizArticle bizArticle);

    /**
     * 批量删除文章
     *
     * @param ids
     * @return
     */
    int deleteBatch(String[] ids);

    /**
     * 根据categoryId获取文章列表
     *
     * @param categoryId
     * @return
     */
    List<BizArticle> selectByCategoryId(String categoryId);


    /**
     * 搜索文章
     *
     * @param keyWord
     */
    List<BizArticleSearchVo> search(String keyword);

    /**
     * 批量下载文章
     *
     * @param ids
     * @param response
     */
    void downloadArticles(List<String> ids, HttpServletResponse response);
}
