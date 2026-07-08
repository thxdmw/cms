package com.thx.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 文章分类，支持通过 pid 组织成树形结构（顶层分类 pid 为 "0"）。
 * parent/children 用于后台管理页把扁平数据组装成树展示，不是数据库里真实存在的字段。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizCategory extends BaseVo {
    private static final long serialVersionUID = -3409570480708571338L;

    /** 父分类 id，顶层分类固定为 "0" */
    private String pid;
    /** 分类名称 */
    private String name;
    /** 分类描述 */
    private String description;
    /** 排序值，越小越靠前 */
    private Integer sort;
    /** 状态：1 有效 0 无效 */
    private Integer status;
    /** 分类图标（FontAwesome class） */
    private String icon;

    /** 父分类对象，组装树形结构时临时挂载，非本表字段 */
    @TableField(exist = false)
    private BizCategory parent;
    /** 子分类列表，组装树形结构时临时挂载，非本表字段 */
    @TableField(exist = false)
    private List<BizCategory> children;

}
