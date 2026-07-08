package com.thx.module.admin.vo.base;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 分页列表接口的统一返回结构，配合前端表格组件的 rows/total 约定（{@link com.thx.common.util.ResultUtil#table}
 * 是构造它的标准入口）。
 */
@Data
@AllArgsConstructor
public class PageResultVo {
    /** 当前页数据 */
    private List rows;
    /** 总记录数 */
    private Long total;

}
