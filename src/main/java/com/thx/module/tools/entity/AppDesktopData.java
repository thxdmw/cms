package com.thx.module.tools.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "app_desktop_data")
public class AppDesktopData extends BaseVo {

    private String title;
    private String url;
    private String icon;

}
