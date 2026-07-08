package com.thx.module.admin.entity;

import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 前台站点主题，对应 src/main/resources/templates/theme/ 下的一套模板；status 标记的那条记录
 * 是当前站点正在使用的主题（同一时刻只应有一个启用状态的主题）。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizTheme extends BaseVo {
    private static final long serialVersionUID = -1364438867316136662L;

    /** 主题名称 */
    private String name;

    /** 主题描述 */
    private String description;

    /** 主题预览图地址 */
    private String img;

    /** 是否启用：1 启用 0 未启用 */
    private Integer status;

}