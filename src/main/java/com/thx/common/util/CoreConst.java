package com.thx.common.util;

import lombok.experimental.UtilityClass;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 全项目共用的通用常量。响应状态码不在这里定义，统一放在 {@link com.thx.enums.ResponseStatus}。
 */
@UtilityClass
public class CoreConst {

    /** 通用有效状态 */
    public static final Integer STATUS_VALID = 1;
    /** 通用无效/已删除状态 */
    public static final Integer STATUS_INVALID = 0;
    /** 默认分页大小 */
    public static final Integer PAGE_SIZE = 10;
    /** 树形菜单/分类顶层节点的父级 id 取值 */
    public static final String TOP_MENU_ID = "0";
    /** 顶层菜单展示名称 */
    public static final String TOP_MENU_NAME = "顶层菜单";
    /** Shiro Session 在 Redis 里的 key 前缀 */
    public static final String SHIRO_REDIS_SESSION_PREFIX = "pb_cms:session:";
    /** Shiro 授权缓存在 Redis 里的缓存名 */
    public static final String SHIRO_REDIS_CACHE_NAME = "shiro_pb_cms";

    /** 后台静态资源/路由前缀 */
    public static final String ADMIN_PREFIX = "admin/";

    /** 前台主题模板/资源前缀 */
    public static final String THEME_PREFIX = "theme/";

    /**
     * 是否启用演示模拟数据
     */
    public static final boolean ENABLE_DEMO_DATA = true;

    /**
     * 网站是否静态化
     */
    public static final String SITE_STATIC_KEY = "SITE_STATIC";
    public static final AtomicBoolean SITE_STATIC = new AtomicBoolean(false);
}
