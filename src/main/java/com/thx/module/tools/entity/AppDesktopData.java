package com.thx.module.tools.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * "应用桌面"条目实体，对应表 {@code app_desktop_data}（表注释：应用表）。
 * 每条记录代表一个展示在前台"工具箱/应用桌面"页面上的应用快捷方式：名称 + 跳转地址 + 图标，
 * 供 {@link com.thx.module.tools.controller.AppDesktopDataController} 做简单的增删改查管理。
 * <p>
 * 继承的是 admin 模块下的 {@link BaseVo}（而不是 tools 模块自己的基类）——这是跨模块的历史遗留依赖，
 * 由此获得 id / createTime / updateTime 三个通用字段，这里不做调整。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "app_desktop_data")
public class AppDesktopData extends BaseVo {

    /** 应用名称 */
    private String title;
    /** 应用跳转地址（URL） */
    private String url;
    /** 应用图标地址 */
    private String icon;

}
