package com.thx.module.file.service;

import com.thx.module.file.model.FilePolicy;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件策略服务
 * 负责 appId+namespace -> FilePolicy 的查找，以及上传文件是否符合策略的校验
 */
public interface FilePolicyService {

    /**
     * 根据 appId + namespace 查找有效的文件策略
     * App 不存在 / Namespace 不存在 / Policy 不存在 / Policy 已禁用，一律拒绝，不做任何 fallback
     */
    FilePolicy getPolicy(String appId, String namespace);

    /**
     * 校验文件是否符合策略：大小、扩展名、声明的 Content-Type、Tika 检测出的真实 MIME、访问级别配置
     */
    void validate(MultipartFile file, FilePolicy policy);
}
