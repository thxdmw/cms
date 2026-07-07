package com.thx.module.file.storage;

import com.thx.module.file.config.FileSystemProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * MinIO 版本的对象存储客户端
 * 只调用 putObject / removeObject / getPresignedObjectUrl，
 * 不生成 Object Key、不决定 Bucket、不创建 Bucket、不设置 Bucket Policy
 *
 * 内网/外网地址分离：
 * putObject/removeObject 用内网 endpoint（minioClient），追求速度，
 * 只有部署在同一网络的服务器需要访问得到；
 * 生成 Presigned URL 必须用外部客户端能解析到的地址作为 host，
 * 否则拿到的链接只有内网机器自己能打开，所以单独用
 * cms.file-system.public-domain 构造 presignMinioClient，
 * 两个 MinioClient 用同一套 Access Key / Secret Key，
 * 只是 endpoint 不同，构造过程本身不需要网络连接。
 */
@Slf4j
@Component
public class MinioObjectStorageClient implements ObjectStorageClient {

    /** MinIO 官方 SDK 客户端，内网地址，用于 putObject/removeObject 等实际数据操作 */
    private final MinioClient minioClient;

    /** 专门用于生成 Presigned URL 的客户端，host 是外部可达的 public-domain */
    private final MinioClient presignMinioClient;

    public MinioObjectStorageClient(MinioClient minioClient, FileSystemProperties fileSystemProperties) {
        this.minioClient = minioClient;
        this.presignMinioClient = buildPresignClient(minioClient, fileSystemProperties);
    }

    private static MinioClient buildPresignClient(MinioClient internalClient, FileSystemProperties properties) {
        String publicDomain = properties.getPublicDomain();
        if (publicDomain == null || publicDomain.trim().isEmpty()) {
            log.warn("cms.file-system.public-domain 未配置，私有文件的 Presigned URL 将使用内网 endpoint 生成，"
                    + "外部客户端很可能无法访问，请尽快配置对外可达的地址");
            return internalClient;
        }
        FileSystemProperties.MinioConfig minioConfig = properties.getStorage().getMinio();
        return MinioClient.builder()
                .endpoint(publicDomain.trim())
                .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                .build();
    }

    @Override
    public StoragePutResult put(String bucket, String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            ObjectWriteResponse response = minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            return new StoragePutResult(bucket, objectKey, response.etag());
        } catch (Exception e) {
            throw new IllegalStateException("上传对象到存储失败: bucket=" + bucket, e);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (ErrorResponseException e) {
            if (!"NoSuchKey".equals(e.errorResponse().code())) {
                throw new IllegalStateException("删除对象失败: bucket=" + bucket, e);
            }
            log.debug("对象已不存在，视为删除成功: bucket={}, objectKey={}", bucket, objectKey);
        } catch (Exception e) {
            throw new IllegalStateException("删除对象失败: bucket=" + bucket, e);
        }
    }

    @Override
    public String presignGet(String bucket, String objectKey, int expireSeconds) {
        try {
            return presignMinioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .method(Method.GET)
                    .expiry(expireSeconds)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("生成预签名 URL 失败: bucket=" + bucket, e);
        }
    }
}
