package com.thx.module.admin.vo.base;

import lombok.Data;

/**
 * 分页查询条件基类，各业务查询条件 VO（如 ArticleConditionVo）继承它获得统一的分页参数。
 */
@Data
public class BaseConditionVo {
    /** 页码，从 1 开始 */
    private int pageNumber = 1;
    /** 每页条数 */
    private int pageSize = 10;

}
