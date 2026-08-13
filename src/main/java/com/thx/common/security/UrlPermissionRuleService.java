package com.thx.common.security;

import cn.hutool.core.util.StrUtil;
import com.thx.common.config.properties.FileUploadProperties;
import com.thx.common.config.properties.StaticizeProperties;
import com.thx.common.util.CoreConst;
import com.thx.module.admin.entity.Permission;
import com.thx.module.admin.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * URL 级鉴权规则的数据源，取代原先的 {@code ShiroService}。
 * <p>
 * 规则分两部分：写死的匿名放行路径（静态资源、登录页等），以及从数据库 Permission 表
 * 读出的"URL → 权限标识"映射。{@link #updatePermission()} 支持在权限数据变更后
 * 不重启应用即刻生效——这一点与原实现的目标一致，但实现方式简单得多：
 * 原先需要反射式地操作 Shiro 内部的 {@code DefaultFilterChainManager} 重建过滤器链，
 * 现在只需替换本类持有的规则列表，拦截器每次请求都会读取最新值。
 * <p>
 * 规则列表用 {@code volatile} 持有并整体替换（而非原地修改），
 * 保证并发读取时不会看到构建到一半的中间状态。
 */
@Slf4j
@Service
// 用 RequiredArgsConstructor 而非 AllArgsConstructor：下面两个 volatile 状态字段
// 是运行时维护的规则缓存，不应出现在构造器参数里
@RequiredArgsConstructor
public class UrlPermissionRuleService {

    private final PermissionService permissionService;

    private final FileUploadProperties fileUploadProperties;

    private final StaticizeProperties staticizeProperties;

    /** 匿名放行路径，启动后不再变化。 */
    private volatile List<String> anonymousPatterns = Collections.emptyList();

    /** 数据库驱动的 URL → 权限标识规则，权限变更时整体替换。 */
    private volatile List<UrlPermissionRule> permissionRules = Collections.emptyList();

    /**
     * 一条 URL 鉴权规则。
     *
     * @param urlPattern URL 匹配模式（Ant 风格）
     * @param permission 访问该 URL 所需的权限标识
     */
    public record UrlPermissionRule(String urlPattern, String permission) {
    }

    @PostConstruct
    public void init() {
        this.anonymousPatterns = buildAnonymousPatterns();
        updatePermission();
    }

    /**
     * 构建匿名放行路径列表，内容与原 Shiro 过滤器链中标记为 {@code anon} 的路径一一对应。
     */
    private List<String> buildAnonymousPatterns() {
        List<String> patterns = new ArrayList<>();
        patterns.add("/");
        patterns.add("/blog/**");
        // 博客前端 Vue SPA 静态资源（index.html/js/libs），无构建、随 static/ 一起打包进 JAR
        patterns.add("/blog-app/**");
        // 后台管理 Vue SPA 静态资源本身公开无害，真正的权限保护在各个 API 接口层
        patterns.add("/admin-app/**");
        patterns.add("/register");
        patterns.add("/login");
        patterns.add("/kickout");
        patterns.add("/error/**");
        patterns.add("/css/**");
        patterns.add("/js/**");
        patterns.add("/img/**");
        patterns.add("/libs/**");
        patterns.add("/favicon.ico");
        patterns.add("/captcha");
        patterns.add("/tools/api/**");
        // 文件系统 /api/v1/files/** 不走会话认证，改由 FileAuthInterceptor 做 API Key 认证
        patterns.add("/api/v1/files/**");
        // GameSave 使用独立的设备 Token，交由 GameDeviceTokenInterceptor 返回 JSON 认证结果
        patterns.add("/api/game-save/v1/**");
        patterns.add(fileUploadProperties.getAccessPathPattern());
        patterns.add(staticizeProperties.getAccessPathPattern());
        return Collections.unmodifiableList(patterns);
    }

    /**
     * 从数据库重新加载 URL 权限规则并原子替换，供权限增删改后调用。
     */
    public void updatePermission() {
        List<Permission> permissionList = permissionService.selectAll(CoreConst.STATUS_VALID);
        List<UrlPermissionRule> rules = new ArrayList<>();
        for (Permission permission : permissionList) {
            if (StrUtil.isAllNotBlank(permission.getUrl(), permission.getPerms())) {
                rules.add(new UrlPermissionRule(permission.getUrl(), permission.getPerms()));
            }
        }
        this.permissionRules = Collections.unmodifiableList(rules);
        log.info("URL 权限规则已刷新，共 {} 条", rules.size());
    }

    /**
     * @return 匿名放行路径列表
     */
    public List<String> getAnonymousPatterns() {
        return anonymousPatterns;
    }

    /**
     * @return 当前生效的 URL 权限规则
     */
    public List<UrlPermissionRule> getPermissionRules() {
        return permissionRules;
    }
}
