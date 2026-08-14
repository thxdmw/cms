package com.thx.module.file.service;

import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.vo.FileInfoResult;

/**
 * 文件内容对象查询能力，供同 JVM 内业务模块做内容去重。
 * 不暴露 Mapper、bucket 或 objectKey，仍由 file 模块执行 App/Owner 权限校验。
 */
public interface FileObjectLookupService {

    /**
     * 按当前 App、namespace、SHA-256 与大小查找 ACTIVE 文件。
     * 未找到返回 null；找到后仍执行读取权限校验。
     */
    FileInfoResult findActiveByHash(String namespace,
                                    String sha256,
                                    long size,
                                    FileCallerContext caller);
}
