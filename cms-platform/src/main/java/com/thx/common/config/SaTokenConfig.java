package com.thx.common.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.thx.common.security.UrlPermissionRuleService;
import com.thx.infra.AnonymousPathScanner;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 鉴权装配，取代原先的 {@code ShiroConfig}。
 * <p>
 * 相比 Shiro 的配置，这里省掉了大量样板：会话管理、Redis 存储、记住我、
 * 同账号最大登录数等都已由 Sa-Token 通过 {@code application.yml} 中的
 * {@code sa-token.*} 配置项内置支持，无需再手写 SessionManager / SessionDAO /
 * CacheManager / RememberMeManager 等 Bean。
 * <p>
 * <b>鉴权顺序与原 Shiro 过滤器链保持一致</b>（先匹配先生效）：
 * <ol>
 *   <li>静态匿名路径（登录页、静态资源、走独立认证的 API）直接放行；</li>
 *   <li>带 {@code @AnonymousAccess} 注解的接口放行；</li>
 *   <li>数据库中配置了权限标识的 URL，校验对应权限；</li>
 *   <li>其余全部路径要求已登录。</li>
 * </ol>
 * 原先每条权限规则后面都要额外挂一个 {@code kickout} 过滤器来做"同账号登录数限制"，
 * 现在该能力由 Sa-Token 的 {@code max-login-count} 内置提供，被挤下线的会话
 * 在 {@code checkLogin()} 时会抛出 NotLoginException，由全局异常处理统一响应。
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final UrlPermissionRuleService urlPermissionRuleService;

    /**
     * 匿名路径扫描器在容器刷新完成后才填充数据，这里用 {@link Lazy} 避免启动期的初始化顺序问题。
     */
    @Lazy
    private final AnonymousPathScanner anonymousPathScanner;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 0. CORS 预检请求（OPTIONS）不做登录鉴权，统一放行交由 Spring CORS 处理器应答。
            // 否则跨域调用受保护接口（如 /currentUser、/tools/api/appDesktopData/add）时，
            // 未登录的预检请求会被下方 checkLogin 拦截，异常处理将其变成 302 / 无 CORS 头，
            // 浏览器会报 "Response to preflight request doesn't pass access control check"。
            if ("OPTIONS".equalsIgnoreCase(SaHolder.getRequest().getMethod())) {
                SaRouter.stop();
            }

            // 1. 静态匿名放行路径
            SaRouter.match(urlPermissionRuleService.getAnonymousPatterns()).stop();

            // 2. @AnonymousAccess 注解标记的接口（按 请求方法 + URI 精确匹配）
            SaRequest request = SaHolder.getRequest();
            if (anonymousPathScanner.isAnonymous(request.getMethod(), request.getRequestPath())) {
                SaRouter.stop();
            }

            // 3. 数据库配置的 URL 级权限
            for (UrlPermissionRuleService.UrlPermissionRule rule : urlPermissionRuleService.getPermissionRules()) {
                SaRouter.match(rule.urlPattern())
                        .check(() -> StpUtil.checkPermission(rule.permission()));
            }

            // 4. 兜底：其余路径一律要求登录
            SaRouter.match("/**").check(() -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
