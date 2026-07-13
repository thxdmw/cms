package com.thx.common.shiro;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.thx.common.config.properties.FileUploadProperties;
import com.thx.common.config.properties.StaticizeProperties;
import com.thx.common.util.CoreConst;
import com.thx.module.admin.entity.Permission;
import com.thx.module.admin.service.PermissionService;
import lombok.AllArgsConstructor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.servlet.AbstractShiroFilter;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 组装并动态刷新 Shiro 的过滤器链规则：一部分是写死的匿名放行路径（静态资源、登录页等），
 * 另一部分是从数据库 Permission 表读出来、按 perms 权限标识动态生成的规则。
 * updatePermission() 允许在权限数据变更后（如新增菜单/修改权限标识）不重启应用就让规则生效，
 * 做法是直接操作 Shiro 内部的 FilterChainManager 重新构建过滤器链。
 */
@Service
@AllArgsConstructor
public class ShiroService {

    private final PermissionService permissionService;

    private final ShiroFilterFactoryBean shiroFilterFactoryBean;

    private final FileUploadProperties fileUploadProperties;
    private final StaticizeProperties staticizeProperties;

    @PostConstruct
    public void init() {
        updatePermission();
    }

    public Map<String, String> loadFilterChainDefinitions() {
        Map<String, String> filterChainDefinitionMap = MapUtil.newHashMap(true);

        // 1. 静态匿名放行路径
        filterChainDefinitionMap.put("/", "anon");
        filterChainDefinitionMap.put("/blog/**", "anon");
        // 博客前端 Vue SPA 静态资源（index.html/js/libs），无构建、随 static/ 一起打包进 JAR
        filterChainDefinitionMap.put("/blog-app/**", "anon");
        // 后台管理 Vue SPA 静态资源本身公开无害，真正的权限保护在各个 API 接口层
        filterChainDefinitionMap.put("/admin-app/**", "anon");
        filterChainDefinitionMap.put("/register", "anon");
        filterChainDefinitionMap.put("/login", "anon");
        filterChainDefinitionMap.put("/kickout", "anon");
        filterChainDefinitionMap.put("/error/**", "anon");
        filterChainDefinitionMap.put("/css/**", "anon");
        filterChainDefinitionMap.put("/js/**", "anon");
        filterChainDefinitionMap.put("/img/**", "anon");
        filterChainDefinitionMap.put("/libs/**", "anon");
        filterChainDefinitionMap.put("/favicon.ico", "anon");
        filterChainDefinitionMap.put("/captcha", "anon");
        filterChainDefinitionMap.put("/tools/api/**", "anon");
        // 文件系统 /api/v1/files/** 不走 Shiro 会话认证，改由 FileAuthInterceptor 做 API Key 认证
        filterChainDefinitionMap.put("/api/v1/files/**", "anon");
        // GameSave 使用独立的设备 Token；必须绕过 Shiro 会话过滤器，交由 GameDeviceTokenInterceptor 返回 JSON 认证结果。
        filterChainDefinitionMap.put("/api/game-save/v1/**", "anon");
        filterChainDefinitionMap.put(fileUploadProperties.getAccessPathPattern(), "anon");
        filterChainDefinitionMap.put(staticizeProperties.getAccessPathPattern(), "anon");

        // 2. 数据库权限控制
        List<Permission> permissionList = permissionService.selectAll(CoreConst.STATUS_VALID);
        for (Permission permission : permissionList) {
            if (StrUtil.isAllNotBlank(permission.getUrl(), permission.getPerms())) {
                String perm = "perms[" + permission.getPerms() + ']';
                filterChainDefinitionMap.put(permission.getUrl(), perm + ",kickout");
            }
        }

        // 3. 默认匹配所有路径，走自定义 Filter（支持注解），并加 kickout
        //filterChainDefinitionMap.put("/**", "user,kickout");
        filterChainDefinitionMap.put("/**", "annoOrLogin,kickout");

        return filterChainDefinitionMap;
    }

    public void updatePermission() {
        synchronized (shiroFilterFactoryBean) {
            AbstractShiroFilter shiroFilter;
            try {
                shiroFilter = (AbstractShiroFilter) shiroFilterFactoryBean.getObject();
            } catch (Exception e) {
                throw new RuntimeException("get ShiroFilter from shiroFilterFactoryBean error!");
            }

            PathMatchingFilterChainResolver filterChainResolver = (PathMatchingFilterChainResolver) shiroFilter.getFilterChainResolver();
            DefaultFilterChainManager manager = (DefaultFilterChainManager) filterChainResolver.getFilterChainManager();

            // 清空原有配置
            manager.getFilterChains().clear();
            shiroFilterFactoryBean.getFilterChainDefinitionMap().clear();

            // 加载新配置
            shiroFilterFactoryBean.setFilterChainDefinitionMap(loadFilterChainDefinitions());
            Map<String, String> chains = shiroFilterFactoryBean.getFilterChainDefinitionMap();
            chains.forEach((url, perm) -> manager.createChain(url, StrUtil.cleanBlank(perm)));
        }
    }

}
