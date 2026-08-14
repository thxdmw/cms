package com.thx.module.file.service;

import com.thx.module.file.model.FilePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实连接本地 MySQL 的集成测试
 * 目的是验证 MyBatis-Plus 下划线转驼峰的字段映射（如 max_file_size -> maxFileSize）
 * 在真实数据库往返中确实生效，而不仅仅是 Mock 单元测试里假设成立
 */
@SpringBootTest
class FilePolicyServiceIntegrationTest {

    @Autowired
    private FilePolicyService filePolicyService;

    @Test
    void resolvesSeededCmsArticleImagePolicyWithCorrectFieldMapping() {
        FilePolicy policy = filePolicyService.getPolicy("cms", "article-image");

        assertEquals("PUBLIC_IMAGE", policy.getPolicyCode());
        assertEquals(10485760L, policy.getMaxFileSize());
        assertEquals("PUBLIC", policy.getAccessLevel());
        assertEquals("public-assets", policy.getBucket());
        assertTrue(policy.allowedExtensionSet().contains("jpg"));
        assertTrue(policy.allowedMimeTypeSet().contains("image/png"));
    }

    @Test
    void resolvesSeededCmsAttachmentPolicy() {
        FilePolicy policy = filePolicyService.getPolicy("cms", "attachment");

        assertEquals("PRIVATE_FILE", policy.getPolicyCode());
        assertEquals("OWNER_ONLY", policy.getAccessLevel());
        assertEquals("private-files", policy.getBucket());
    }
}
