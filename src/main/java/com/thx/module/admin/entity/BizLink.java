package com.thx.module.admin.entity;

import com.thx.module.admin.vo.base.BaseVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 友情链接
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class BizLink extends BaseVo {
    private static final long serialVersionUID = -6511423333796987519L;

    /** 链接地址 */
    private String url;
    /** 站点名称 */
    private String name;
    /** 站点描述 */
    private String description;
    /** 站点 Logo/图标地址 */
    private String img;
    /** 联系邮箱 */
    private String email;
    /** 联系 QQ */
    private String qq;
    /** 状态：1 展示 0 隐藏 */
    private Integer status;
    /** 来源：区分是站长自己添加还是对方申请等（具体取值由后台维护） */
    private Integer origin;
    /** 备注 */
    private String remark;

}
