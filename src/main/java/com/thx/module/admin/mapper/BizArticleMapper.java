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
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public interface BizArticleMapper extends BaseMapper<BizArticle> {

    /**
     * 分页查询，关联查询文章标签、文章类型
     *
     * @param page
     * @param vo
     * @return
     */
    List<BizArticle> findByCondition(@Param("page") IPage<BizArticle> page, @Param("vo") ArticleConditionVo vo);

    /**
     * 统计指定文章的标签集合
     *
     * @param list
     * @return
     */
    List<BizArticle> listTagsByArticleId(List<String> list);

    /**
     * 热门文章
     *
     * @param page
     * @return
     */
    List<BizArticle> hotList(@Param("page") IPage<BizArticle> page);

    /**
     * 获取文章详情，文章标签、文章类型
     *
     * @param id
     * @return
     */
    BizArticle getById(String id);

    /**
     * 批量删除文章
     *
     * @param ids
     * @return
     */
    int deleteBatch(String[] ids);

    /**
     * 统计网站信息
     *
     * @return
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
