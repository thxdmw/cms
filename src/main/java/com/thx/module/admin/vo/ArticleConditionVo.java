package com.thx.module.admin.vo;

import com.thx.module.admin.vo.base.BaseConditionVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 文章查询条件封装（前台按分类/标签/推荐等维度筛选文章列表时使用），继承 BaseConditionVo 的分页参数。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class ArticleConditionVo extends BaseConditionVo {
    /** 按分类过滤 */
    private String categoryId;
    /** 按单个标签过滤 */
    private String tagId;
    /** 按文章状态过滤：1 已发布 0 草稿 */
    private Integer status;
    /** 只查置顶文章 */
    private Boolean top;
    /** 只查推荐文章 */
    private Boolean recommended;
    /** 只查参与轮播的文章 */
    private Boolean slider;
    /** 只查原创文章 */
    private Boolean original;
    /** 是否随机排序（如首页"随机推荐"场景） */
    private Boolean random;
    /** 是否只查最近发布的文章 */
    private Boolean recentFlag;
    /** 是否只查轮播标记的文章（和 slider 语义类似，具体使用场景以调用处为准） */
    private Boolean sliderFlag;
    /** 按多个标签过滤 */
    private List<Long> tagIds;
    /** 关键字模糊匹配标题/内容 */
    private String keywords;

}

