package com.thx.common.config;

import cn.hutool.core.util.StrUtil;
import com.thx.common.config.properties.CorsProperties;
import com.thx.common.config.properties.FileUploadProperties;
import com.thx.common.config.properties.StaticizeProperties;
import com.thx.common.intercepter.AgentApiAuthInterceptor;
import com.thx.common.intercepter.CommonDataInterceptor;
import com.thx.common.intercepter.RequestLoggingInterceptor;
import com.thx.module.file.interceptor.FileAuthInterceptor;
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

/**
 * Created with IntelliJ IDEA.
 *
 * @author tanghaixin
 * @date 2020/4/18 11:58 上午
 */
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
    //跨域配置
    private final CorsProperties corsProperties;

    /**
     * 配置本地文件上传的虚拟路径和静态化的文件生成路径
     * 备注：这是一种图片上传访问图片的方法，实际上也可以使用nginx反向代理访问图片
     *
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 文件上传
        String uploadFolder = fileUploadProperties.getUploadFolder();
        uploadFolder = StrUtil.appendIfMissing(uploadFolder, File.separator);
        registry.addResourceHandler(fileUploadProperties.getAccessPathPattern())
                .addResourceLocations("file:" + uploadFolder);
        // 静态化
        String staticFolder = staticizeProperties.getFolder();
        staticFolder = StrUtil.appendIfMissing(staticFolder, File.separator);
        registry.addResourceHandler(staticizeProperties.getAccessPathPattern())
                .addResourceLocations("file:" + staticFolder);
        // 博客 Vue SPA 静态资源单独关闭强缓存：开发迭代期间文件改动频繁，
        // 默认的 Last-Modified 协商缓存在部分浏览器上仍可能命中启发式缓存而不发出校验请求，
        // 这里显式要求每次都带条件请求校验，避免刷新后还是拿到旧文件
        registry.addResourceHandler("/blog-app/**")
                .addResourceLocations("classpath:/static/blog-app/")
                .setCacheControl(CacheControl.noCache());
        // 后台管理 Vue SPA 静态资源，同样关闭强缓存，原因同上
        registry.addResourceHandler("/admin-app/**")
                .addResourceLocations("classpath:/static/admin-app/")
                .setCacheControl(CacheControl.noCache());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(commonDataInterceptor).addPathPatterns("/**");
        registry.addInterceptor(requestLoggingInterceptor).addPathPatterns("/**");
        // Agent API 认证拦截器（只对 /agent/api/** 生效）
        registry.addInterceptor(agentApiAuthInterceptor)
                .addPathPatterns("/agent/api/**");
        // 文件系统 API Key 认证拦截器（只对 /api/v1/files/** 生效）
        registry.addInterceptor(fileAuthInterceptor)
                .addPathPatterns("/api/v1/files/**");
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
