package com.thx.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 站点配置键值对，sysKey 对应 {@link com.thx.enums.SysConfigKey} 枚举定义的各个配置项
 * （网站名称、SEO 描述、站长信息等），后台"站点设置"页面读写的就是这张表。
 */
@Data
@Accessors(chain = true)
public class SysConfig implements Serializable {

    private static final long serialVersionUID = -1645880315099183738L;

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * key
     */
    private String sysKey;

    /**
     * value
     */
    private String sysValue;

    /**
     * 状态  1：有效 0：无效
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

}