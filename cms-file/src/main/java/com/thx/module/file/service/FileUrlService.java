package com.thx.module.file.service;

import com.thx.module.file.config.FileSystemProperties;
import com.thx.module.file.enums.FileAccessLevel;
import com.thx.module.file.model.FileAsset;
import com.thx.module.file.storage.ObjectStorageClient;
import com.thx.module.file.util.UrlJoinUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 文件访问 URL 动态生成
 * 数据库不保存 url / presigned_url，PUBLIC 文件拼接公开域名，
 * 其余访问级别一律生成 Presigned GET URL
 */
@Service
@RequiredArgsConstructor
public class FileUrlService {

    /** 用于生成私有文件的 Presigned GET URL */
    private final ObjectStorageClient objectStorageClient;
    /** 读取公开域名、预签名 URL 过期时间等配置 */
    private final FileSystemProperties fileSystemProperties;

    /** 根据文件的访问级别动态生成访问 URL，不读取任何持久化的 url 字段 */
    public String resolveUrl(FileAsset asset) {
        FileAccessLevel accessLevel = FileAccessLevel.valueOf(asset.getAccessLevel());
        if (accessLevel == FileAccessLevel.PUBLIC) {
            return UrlJoinUtil.join(fileSystemProperties.getPublicDomain(), asset.getBucket(), asset.getObjectKey());
        }
        return objectStorageClient.presignGet(
                asset.getBucket(), asset.getObjectKey(), fileSystemProperties.getPresignedUrlExpireSeconds());
    }
}
