package com.thx.module.blog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 博客前端页面路由。
 * 页面本身已经改造成 Vue 3 SPA（静态资源见 static/blog-app/），本身不再服务端渲染 Thymeleaf 模板，
 * 这里只是把旧的 URL 全部转发到 SPA 壳（index.html），由 Vue Router 在浏览器端接管路由解析。
 * 保留这些具体的 URL 映射（而不是用一个通配符）是为了和旧链接完全兼容，不影响 /admin/**、/api/** 等其他路由。
 */
@Controller
public class BlogWebController {

    /**
     * 统一转发到 SPA 壳页面，具体渲染哪个页面由前端 Vue Router 根据当前 URL 决定
     */
    @GetMapping({
            "/",
            "/blog/index/{pageNumber}",
            "/blog/category/{categoryId}",
            "/blog/category/{categoryId}/{pageNumber}",
            "/blog/tag/{tagId}",
            "/blog/tag/{tagId}/{pageNumber}",
            "/blog/article/{articleId}",
            "/blog/comment"
    })
    public String blogApp() {
        return "forward:/blog-app/index.html";
    }

}
