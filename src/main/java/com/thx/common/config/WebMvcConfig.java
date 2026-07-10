package com.thx.common.config;

import cn.hutool.core.util.StrUtil;
import com.thx.common.config.properties.CorsProperties;
import com.thx.common.config.properties.FileUploadProperties;
import com.thx.common.config.properties.StaticizeProperties;
import com.thx.common.interceptor.AgentApiAuthInterceptor;
import com.thx.common.interceptor.CommonDataInterceptor;
import com.thx.common.interceptor.RequestLoggingInterceptor;
import com.thx.module.file.interceptor.FileAuthInterceptor;
import com.thx.module.gamesave.interceptor.GameDeviceTokenInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.util.List;

/** 项目统一 Spring MVC 配置：静态资源、拦截器、跨域和消息转换器均在此注册。 */
@Configuration
@AllArgsConstructor
@EnableConfigurationProperties({FileUploadProperties.class, StaticizeProperties.class})
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileUploadProperties fileUploadProperties;
    private final StaticizeProperties staticizeProperties;
    private final CommonDataInterceptor commonDataInterceptor;
    private final RequestLoggingInterceptor requestLoggingInterceptor;
    private final AgentApiAuthInterceptor agentApiAuthInterceptor;
    private final FileAuthInterceptor fileAuthInterceptor;
    private final GameDeviceTokenInterceptor gameDeviceTokenInterceptor;
    private final CorsProperties corsProperties;

    /** 配置本地上传目录、静态化目录和两个 Vue SPA 的静态资源缓存策略。 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadFolder = fileUploadProperties.getUploadFolder();
        uploadFolder = StrUtil.appendIfMissing(uploadFolder, File.separator);
        registry.addResourceHandler(fileUploadProperties.getAccessPathPattern())
                .addResourceLocations("file:" + uploadFolder);

        String staticFolder = staticizeProperties.getFolder();
        staticFolder = StrUtil.appendIfMissing(staticFolder, File.separator);
        registry.addResourceHandler(staticizeProperties.getAccessPathPattern())
                .addResourceLocations("file:" + staticFolder);

        registry.addResourceHandler("/blog-app/**")
                .addResourceLocations("classpath:/static/blog-app/")
                .setCacheControl(CacheControl.noCache());
        registry.addResourceHandler("/admin-app/**")
                .addResourceLocations("classpath:/static/admin-app/")
                .setCacheControl(CacheControl.noCache());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(commonDataInterceptor).addPathPatterns("/**");
        registry.addInterceptor(requestLoggingInterceptor).addPathPatterns("/**");

        // Agent API 独立 API Key 认证。
        registry.addInterceptor(agentApiAuthInterceptor)
                .addPathPatterns("/agent/api/**");

        // 通用文件系统 App API Key 认证。
        registry.addInterceptor(fileAuthInterceptor)
                .addPathPatterns("/api/v1/files/**");

        // GameSave 使用用户级设备 Token；登录接口是唯一免设备 Token 的 GameSave API。
        registry.addInterceptor(gameDeviceTokenInterceptor)
                .addPathPatterns("/api/game-save/v1/**")
                .excludePathPatterns("/api/game-save/v1/auth/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(corsProperties.getAllowedOriginPatterns().toArray(new String[0]))
                .allowedMethods(corsProperties.getAllowedMethods().toArray(new String[0]))
                .allowedHeaders(corsProperties.getAllowedHeaders().toArray(new String[0]))
                .allowCredentials(corsProperties.getAllowCredentials())
                .maxAge(corsProperties.getMaxAge());
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter());
    }
}
