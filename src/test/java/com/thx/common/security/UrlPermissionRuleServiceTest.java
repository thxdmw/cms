package com.thx.common.security;

import com.thx.common.config.properties.FileUploadProperties;
import com.thx.common.config.properties.StaticizeProperties;
import com.thx.common.util.CoreConst;
import com.thx.module.admin.entity.Permission;
import com.thx.module.admin.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 鉴权规则边界测试，接替原先的 {@code ShiroServiceTest}。
 * <p>
 * 重点是守住那些<b>必须绕过会话认证</b>的路径：这些接口走的是各自独立的认证机制
 * （API Key、设备 Token），一旦被会话拦截器接管就会直接不可用，属于回归风险很高的点。
 */
class UrlPermissionRuleServiceTest {

    private PermissionService permissionService;
    private UrlPermissionRuleService service;

    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);
        when(permissionService.selectAll(CoreConst.STATUS_VALID)).thenReturn(Collections.emptyList());

        FileUploadProperties fileUploadProperties = mock(FileUploadProperties.class);
        when(fileUploadProperties.getAccessPathPattern()).thenReturn("/upload/**");
        StaticizeProperties staticizeProperties = mock(StaticizeProperties.class);
        when(staticizeProperties.getAccessPathPattern()).thenReturn("/static/**");

        service = new UrlPermissionRuleService(permissionService, fileUploadProperties, staticizeProperties);
        service.init();
    }

    @Test
    @DisplayName("GameSave 设备 Token 接口必须绕过会话认证")
    void gameSaveApiMustBypassSessionAuth() {
        assertTrue(service.getAnonymousPatterns().contains("/api/game-save/v1/**"),
                "GameSave 走独立设备 Token 认证，必须在匿名放行列表中");
    }

    @Test
    @DisplayName("文件系统 API Key 接口必须绕过会话认证")
    void fileApiMustBypassSessionAuth() {
        assertTrue(service.getAnonymousPatterns().contains("/api/v1/files/**"),
                "文件系统走 API Key 认证，必须在匿名放行列表中");
    }

    @Test
    @DisplayName("健康检查端点必须匿名放行，否则部署健康检查会一直失败并触发回滚")
    void actuatorHealthMustBeAnonymous() {
        assertTrue(service.getAnonymousPatterns().contains("/actuator/health"),
                "deploy.sh 依赖 /actuator/health 判断新版本是否就绪，被拦截会导致每次部署都误判失败");
    }

    @Test
    @DisplayName("登录页与静态资源匿名放行")
    void loginAndStaticResourcesAreAnonymous() {
        List<String> anon = service.getAnonymousPatterns();
        assertTrue(anon.containsAll(Arrays.asList(
                "/login", "/kickout", "/register", "/captcha",
                "/css/**", "/js/**", "/img/**", "/libs/**", "/blog/**")));
        // 由配置项注入的两个路径也必须在内
        assertTrue(anon.contains("/upload/**"));
        assertTrue(anon.contains("/static/**"));
    }

    @Test
    @DisplayName("数据库权限规则被正确加载，且只保留 url 与 perms 都非空的记录")
    void permissionRulesAreLoadedAndFiltered() {
        Permission valid = new Permission();
        valid.setUrl("/user/list");
        valid.setPerms("user:list");

        Permission missingPerms = new Permission();
        missingPerms.setUrl("/role/list");
        missingPerms.setPerms("  ");

        Permission missingUrl = new Permission();
        missingUrl.setUrl(null);
        missingUrl.setPerms("role:list");

        when(permissionService.selectAll(CoreConst.STATUS_VALID))
                .thenReturn(Arrays.asList(valid, missingPerms, missingUrl));

        service.updatePermission();

        List<UrlPermissionRuleService.UrlPermissionRule> rules = service.getPermissionRules();
        assertEquals(1, rules.size(), "只有 url 与 perms 均非空的记录才应生成规则");
        assertEquals("/user/list", rules.get(0).urlPattern());
        assertEquals("user:list", rules.get(0).permission());
    }

    @Test
    @DisplayName("刷新权限后规则整体替换，不残留旧规则")
    void updatePermissionReplacesRulesAtomically() {
        Permission first = new Permission();
        first.setUrl("/a");
        first.setPerms("a:read");
        when(permissionService.selectAll(CoreConst.STATUS_VALID)).thenReturn(Collections.singletonList(first));
        service.updatePermission();
        assertEquals(1, service.getPermissionRules().size());

        // 模拟权限被删除后刷新
        when(permissionService.selectAll(CoreConst.STATUS_VALID)).thenReturn(Collections.emptyList());
        service.updatePermission();
        assertTrue(service.getPermissionRules().isEmpty(), "旧规则不应残留");
    }
}
