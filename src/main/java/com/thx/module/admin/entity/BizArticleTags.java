package com.thx.module.admin.entity;

import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizArticleTags extends BaseVo {
    private static final long serialVersionUID = 2627147974506469978L;

    private String tagId;
    private String articleId;

}
