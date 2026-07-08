package com.thx.module.admin.entity;

import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章标签，通过 BizArticleTags 关联表和文章多对多关联。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizTags extends BaseVo {
    private static final long serialVersionUID = 3578477956306175100L;

    /** 标签名称 */
    private String name;
    /** 标签描述 */
    private String description;

}
