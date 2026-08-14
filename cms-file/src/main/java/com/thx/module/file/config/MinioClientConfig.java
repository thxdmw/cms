package com.thx.module.file.config;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置
 * 当 cms.file-system.enabled = true 时自动配置 MinioClient Bean
 */
@Configuration
@EnableConfigurationProperties(FileSystemProperties.class)
@ConditionalOnProperty(prefix = "cms.file-system", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MinioClientConfig {

    /**
     * 创建 MinIO 客户端 Bean
     * 如果容器中已有 MinioClient 实例则跳过
     */
    @Bean
    @ConditionalOnMissingBean(MinioClient.class)
    public MinioClient minioClient(FileSystemProperties properties) {
        FileSystemProperties.MinioConfig minio = properties.getStorage().getMinio();
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }
}
