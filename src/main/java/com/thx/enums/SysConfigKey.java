package com.thx.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 站点配置项 Key 枚举，对应 sys_config 表中的 config_key。
 * value 是数据库里实际存储的键名，describe 是后台配置页展示用的中文名称。
 */
@Getter
@AllArgsConstructor
public enum SysConfigKey {
    /**
     * 百度推送地址
     */
    BAIDU_PUSH_URL("BAIDU_PUSH_URL", "百度推送地址"),
    /**
     * 网站名称
     */
    SITE_NAME("SITE_NAME", "网站名称"),
    /**
     * 网站描述
     */
    SITE_DESC("SITE_DESC", "网站描述"),
    /**
     * 网站关键字
     */
    SITE_KWD("SITE_KWD", "网站关键字"),
    /**
     * 站长名称
     */
    SITE_PERSON_NAME("SITE_PERSON_NAME", "站长名称"),
    /**
     * 站长描述
     */
    SITE_PERSON_DESC("SITE_PERSON_DESC", "站长描述"),
    /**
     * 站点logo
     */
    SITE_LOGO("SITE_LOGO", "站点logo"),
    /**
     * 站长头像
     */
    SITE_PERSON_PIC("SITE_PERSON_PIC", "站长头像"),
    ;

    private final String value;
    private final String describe;

}
