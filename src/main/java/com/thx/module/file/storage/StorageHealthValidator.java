package com.thx.module.file.storage;

import com.thx.module.file.config.FileSystemProperties;
import com.thx.module.file.mapper.FilePolicyMapper;
import com.thx.module.file.model.FilePolicy;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 启动时校验 file_policy 中配置的 Bucket 是否存在
 * 生产环境不应该由 Java 应用创建 Bucket / 设置 Policy，这些属于部署脚本（mc、Docker 初始化）的职责，
 * 这里只做存在性检查；仅当 storage.auto-create-bucket=true（建议仅开发环境）时才自动创建，方便本地开发
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageHealthValidator {

    /** 用来直接调用 bucketExists/makeBucket，注意这里绕开了 ObjectStorageClient 抽象，仅供启动校验使用 */
    private final MinioClient minioClient;
    /** 查询所有已配置的文件策略，收集需要校验的 Bucket 列表 */
    private final FilePolicyMapper filePolicyMapper;
    /** 读取 validate-on-startup / auto-create-bucket 开关 */
    private final FileSystemProperties fileSystemProperties;

    /** Spring 容器启动完成后执行 Bucket 存在性校验 */
    @PostConstruct
    public void validate() {
        FileSystemProperties.Storage storage = fileSystemProperties.getStorage();
        if (!storage.isValidateOnStartup()) {
            log.info("文件系统 Bucket 启动校验已关闭（storage.validate-on-startup=false）");
            return;
        }

        Set<String> buckets = collectConfiguredBuckets();
        if (buckets.isEmpty()) {
            log.info("未配置任何文件策略，跳过 Bucket 启动校验");
            return;
        }

        for (String bucket : buckets) {
            checkBucket(bucket, storage.isAutoCreateBucket());
        }
    }

    /** 汇总 file_policy 表中出现过的所有 Bucket 名称（去重） */
    private Set<String> collectConfiguredBuckets() {
        Set<String> buckets = new LinkedHashSet<>();
        for (FilePolicy policy : filePolicyMapper.selectList(null)) {
            if (policy.getBucket() != null && !policy.getBucket().trim().isEmpty()) {
                buckets.add(policy.getBucket().trim());
            }
        }
        return buckets;
    }

    /** 校验单个 Bucket：存在则通过；不存在时按 autoCreate 决定自动创建或直接抛异常终止启动 */
    private void checkBucket(String bucket, boolean autoCreate) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (exists) {
                log.info("文件系统 Bucket 校验通过: {}", bucket);
                return;
            }
            if (autoCreate) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.warn("文件系统 Bucket 不存在，已自动创建（仅建议开发环境使用）: {}", bucket);
                return;
            }
            log.error("文件系统 Bucket 不存在且未开启自动创建，需要运维通过部署脚本预先创建: {}", bucket);
            throw new IllegalStateException("MinIO 存储桶不存在: " + bucket);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("校验 MinIO 存储桶失败: " + bucket, e);
        }
    }
}
