package com.thx.module.file.service.impl;

import cn.hutool.core.io.file.FileNameUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.file.enums.FileAccessLevel;
import com.thx.module.file.exception.FileSystemException;
import com.thx.module.file.mapper.FileAppNamespaceMapper;
import com.thx.module.file.mapper.FilePolicyMapper;
import com.thx.module.file.model.FileAppNamespace;
import com.thx.module.file.model.FilePolicy;
import com.thx.module.file.service.FilePolicyService;
import com.thx.module.file.util.MimeDetector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * FilePolicyService 默认实现
 * appId+namespace -> file_app_namespace -> policy_code -> file_policy，
 * 中间任意一环缺失或被禁用都直接拒绝，不做 ATTACHMENT/OTHER/DEFAULT 之类的兜底
 */
@Service
@RequiredArgsConstructor
public class FilePolicyServiceImpl implements FilePolicyService {

    /** 查询 appId+namespace 对应的策略编码 */
    private final FileAppNamespaceMapper fileAppNamespaceMapper;
    /** 按策略编码查询具体的文件策略配置 */
    private final FilePolicyMapper filePolicyMapper;

    @Override
    public FilePolicy getPolicy(String appId, String namespace) {
        FileAppNamespace ns = fileAppNamespaceMapper.selectOne(
                new LambdaQueryWrapper<FileAppNamespace>()
                        .eq(FileAppNamespace::getAppId, appId)
                        .eq(FileAppNamespace::getNamespace, namespace));
        if (ns == null || ns.getStatus() == null || ns.getStatus() != 1) {
            throw FileSystemException.badRequest("NAMESPACE_NOT_FOUND", "未知的 namespace: " + namespace);
        }

        FilePolicy policy = filePolicyMapper.selectOne(
                new LambdaQueryWrapper<FilePolicy>().eq(FilePolicy::getPolicyCode, ns.getPolicyCode()));
        if (policy == null || policy.getStatus() == null || policy.getStatus() != 1) {
            throw FileSystemException.badRequest("POLICY_DISABLED", "文件策略不可用: " + ns.getPolicyCode());
        }
        return policy;
    }

    @Override
    public void validate(MultipartFile file, FilePolicy policy) {
        if (file == null || file.isEmpty()) {
            throw FileSystemException.badRequest("EMPTY_FILE", "文件不能为空");
        }
        if (file.getSize() > policy.getMaxFileSize()) {
            throw new FileSystemException(413, "FILE_TOO_LARGE", "文件超出大小限制");
        }

        String originalName = file.getOriginalFilename();
        String extension = originalName == null ? "" : FileNameUtil.getSuffix(originalName);
        Set<String> allowedExtensions = policy.allowedExtensionSet();
        if (!allowedExtensions.isEmpty() && !allowedExtensions.contains(extension.toLowerCase())) {
            throw new FileSystemException(415, "EXTENSION_NOT_ALLOWED", "不支持的文件扩展名");
        }

        Set<String> allowedMimeTypes = policy.allowedMimeTypeSet();
        if (!allowedMimeTypes.isEmpty()) {
            String declaredType = file.getContentType();
            if (declaredType == null || !allowedMimeTypes.contains(declaredType.toLowerCase())) {
                throw new FileSystemException(415, "CONTENT_TYPE_NOT_ALLOWED", "不支持的文件类型");
            }
            String detectedType;
            try (InputStream in = file.getInputStream()) {
                detectedType = MimeDetector.detect(in);
            } catch (IOException e) {
                throw new FileSystemException(422, "MIME_DETECT_FAILED", "无法识别文件类型");
            }
            if (detectedType == null || !allowedMimeTypes.contains(detectedType.toLowerCase())) {
                throw new FileSystemException(415, "DETECTED_MIME_NOT_ALLOWED", "文件实际内容类型与允许类型不符");
            }
        }

        try {
            FileAccessLevel.valueOf(policy.getAccessLevel());
        } catch (IllegalArgumentException e) {
            throw FileSystemException.forbidden("文件策略访问级别配置无效");
        }
    }
}
