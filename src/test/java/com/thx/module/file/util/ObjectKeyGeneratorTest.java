package com.thx.module.file.util;

import com.thx.module.file.exception.FileSystemException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ObjectKeyGenerator 对象键生成格式与非法输入（含路径穿越）拒绝测试
 */
class ObjectKeyGeneratorTest {

    @Test
    void generatesExpectedFormat() {
        String key = ObjectKeyGenerator.generate("cms", "article-image", "abc123", "jpg");
        assertTrue(key.startsWith("apps/cms/article-image/"));
        assertTrue(key.endsWith("/abc123.jpg"));
    }

    @Test
    void rejectsPathTraversalInAppId() {
        assertThrows(FileSystemException.class,
                () -> ObjectKeyGenerator.generate("../etc", "attachment", "abc123", "jpg"));
    }

    @Test
    void rejectsUppercaseNamespace() {
        assertThrows(FileSystemException.class,
                () -> ObjectKeyGenerator.generate("cms", "Article-Image", "abc123", "jpg"));
    }

    @Test
    void rejectsPathTraversalInFileId() {
        assertThrows(FileSystemException.class,
                () -> ObjectKeyGenerator.generate("cms", "attachment", "../../secret", "jpg"));
    }

    @Test
    void handlesEmptyExtensionWithoutTrailingDot() {
        String key = ObjectKeyGenerator.generate("cms", "save", "abc123", "");
        assertFalse(key.endsWith("."));
    }
}
