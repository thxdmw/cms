package com.thx.module.admin.controller.system;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * 后台管理页面路由。
 * 后台已经改造成 Vue 3 SPA（静态资源见 static/admin-app/），/admin/** 统一 forward 到 SPA 壳，
 * 由 Vue Router 在浏览器端接管路由解析（干净路径式 URL，如 /admin/articles）。
 * 旧的顶层入口地址（/articles、/tags 等，原来由 AdminRenderController 提供）保留为重定向，
 * 兼容可能已有的书签/外部链接；AdminRenderController 已删除（原方法和这里的重定向路径完全重复，
 * 两个 Controller 同时存在会导致 Spring 启动时报 Ambiguous mapping）。
 * <p>
 * 曾经这里不能用 "/admin/**" 一把梭：Chart.js/SockJS/Stomp 等少数页面按需加载的脚本当时还挂在
 * 旧版 admin 主题目录 /admin/plugins/** 下，@GetMapping 的匹配优先级高于默认静态资源处理器，
 * "/admin/**" 会把这些静态资源请求也截胡、错误地转发成 SPA 壳的 HTML。现在旧版 admin 静态资源
 * （static/admin/）已经整个清理掉，这几个脚本也已经挪进 static/admin-app/libs/ 自己目录下，
 * /admin/** 下面不会再有游离在 admin-app 之外的静态资源，可以放心用通配符。
 * </p>
 */
@Controller
public class AdminWebController {

    @GetMapping({"/admin", "/admin/**"})
    public String adminApp() {
        return "forward:/admin-app/index.html";
    }

    @GetMapping({
            "/workdest", "/users", "/roles", "/permissions", "/online/users",
            "/siteinfo", "/links", "/categories", "/tags", "/articles", "/comments", "/themes",
            "/log/page", "/database/monitoring"
    })
    public String legacyRedirect(HttpServletRequest request) {
        return "redirect:/admin" + request.getRequestURI();
    }

}
