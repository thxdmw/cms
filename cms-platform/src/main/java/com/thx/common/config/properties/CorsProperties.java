package com.thx.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {
    private List<String> allowedOriginPatterns = Collections.emptyList();
    private List<String> allowedMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS");
    // 显式列出跨域带凭证（credentials: include）请求会用到的请求头：
    // 浏览器对"凭证请求 + Access-Control-Allow-Headers: *"的匹配存在限制（* 只对
    // CORS 安全头生效），必须把 Content-Type / Accept / X-Requested-With 显式列出，
    // 否则首页登录、应用桌面等跨域 JSON 请求的预检会失败。
    private List<String> allowedHeaders = Arrays.asList("Content-Type", "Accept", "X-Requested-With", "*");
    private Boolean allowCredentials = true;
    private Long maxAge = 3600L;
}
