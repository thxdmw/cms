package com.thx.module.admin.entity;

import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章与标签的多对多关联表：一篇文章可以有多个标签，一个标签也可以挂在多篇文章上。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizArticleTags extends BaseVo {
    private static final long serialVersionUID = 2627147974506469978L;

    /** 标签 id（关联 BizTags） */
    private String tagId;
    /** 文章 id（关联 BizArticle） */
    private String articleId;

}
