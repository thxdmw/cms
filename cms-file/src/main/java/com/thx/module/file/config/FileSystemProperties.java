package com.thx.module.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件系统配置属性
 * 前缀：cms.file-system
 * 大小限制 / 扩展名 / MIME / 访问级别 / Bucket 等由 file_policy 表配置，
 * App 与 Scope 由 file_app 表配置，这里只保留跨 App 通用的基础设施配置
 */
@Data
@ConfigurationProperties(prefix = "cms.file-system")
public class FileSystemProperties {

    /** 是否启用文件系统模块 */
    private boolean enabled = true;

    /**
     * 对外可达的 MinIO 访问地址，如 https://files.example.com 或 http://&lt;公网IP&gt;:9000。
     * 用途有两个：
     * 1. PUBLIC 文件直接拼接成下载 URL（不签名）
     * 2. 作为生成 Presigned URL 时使用的 host（不能用 storage.minio.endpoint，
     *    那个是内网地址，只有部署在同一网络的服务器自己能访问，
     *    外部浏览器/客户端解析不到）
     * 与 storage.minio.endpoint 允许不同，一个内网一个外网，
     * 未配置时会退化为直接使用内网 endpoint（仅适合前后端都在同一封闭网络的场景）
     */
    private String publicDomain;

    /** 预签名 URL 过期时间（秒） */
    private int presignedUrlExpireSeconds = 3600;

    /** 逻辑删除后的物理清理相关配置 */
    private Cleanup cleanup = new Cleanup();

    /** 对象存储相关配置 */
    private Storage storage = new Storage();

    @Data
    public static class Cleanup {
        /** 是否启用清理定时任务 */
        private boolean enabled = true;
        /** 逻辑删除后的宽限天数，超过后才允许物理清理 */
        private int graceDays = 7;
        /** 清理补偿任务最大重试次数，超过后置为 FAILED 等待人工处理 */
        private int maxRetryCount = 10;
    }

    @Data
    public static class Storage {
        /** 存储提供方，目前仅支持 MINIO */
        private String provider = "MINIO";
        /** 启动时是否校验 file_policy 中配置的 Bucket 是否存在 */
        private boolean validateOnStartup = true;
        /** Bucket 不存在时是否自动创建，生产环境必须为 false */
        private boolean autoCreateBucket = false;
        /** MinIO 连接配置 */
        private MinioConfig minio = new MinioConfig();
    }

    @Data
    public static class MinioConfig {
        /** MinIO 服务端地址，如 http://localhost:9000 */
        private String endpoint;
        /** 访问密钥（Access Key） */
        private String accessKey;
        /** 私密密钥（Secret Key），禁止打印到日志 */
        private String secretKey;
    }
}
