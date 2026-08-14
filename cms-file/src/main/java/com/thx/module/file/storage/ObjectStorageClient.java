package com.thx.module.file.storage;

import java.io.InputStream;

/**
 * 对象存储客户端接口
 * 只认识 bucket / objectKey / InputStream / size / contentType，
 * 不认识 appId / namespace / FilePolicy / ownerId 等业务概念，
 * 便于未来替换为 S3 / 阿里云 OSS 等实现
 */
public interface ObjectStorageClient {

    /**
     * 上传对象
     * @param bucket      存储桶（由调用方根据 FilePolicy 决定，本接口不关心策略）
     * @param objectKey   对象键（由调用方生成）
     * @param inputStream 文件输入流
     * @param size        文件大小（字节）
     * @param contentType MIME 类型
     * @return 写入结果
     */
    StoragePutResult put(String bucket, String objectKey, InputStream inputStream, long size, String contentType);

    /**
     * 删除对象。对象不存在时应当视为删除成功（幂等）
     */
    void delete(String bucket, String objectKey);

    /**
     * 生成预签名 GET URL，用于私有文件的临时访问
     */
    String presignGet(String bucket, String objectKey, int expireSeconds);
}
