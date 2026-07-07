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
    private List<String> allowedHeaders = Arrays.asList("*");
    private Boolean allowCredentials = true;
    private Long maxAge = 3600L;
}
