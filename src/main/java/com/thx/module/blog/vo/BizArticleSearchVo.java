package com.thx.module.blog.vo;

import com.thx.module.admin.vo.base.BaseConditionVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class BizArticleSearchVo extends BaseConditionVo {
    private String id;
    private String title;
    private String skipUrl;
}

