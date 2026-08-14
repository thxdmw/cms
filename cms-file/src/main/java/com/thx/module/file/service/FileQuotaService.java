package com.thx.module.file.service;

/**
 * 文件配额服务
 * 第一版直接 SUM 已用字节数，不做复杂账本；未来高并发场景可替换为账本实现
 */
public interface FileQuotaService {

    /**
     * 校验本次上传是否超出 App 配额，超出则抛出异常
     */
    void checkUploadAllowed(String appId, long uploadSize);
}
