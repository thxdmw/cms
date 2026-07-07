package com.thx.module.file.service.impl;

import com.thx.module.file.exception.FileSystemException;
import com.thx.module.file.mapper.FileAppNamespaceMapper;
import com.thx.module.file.mapper.FilePolicyMapper;
import com.thx.module.file.model.FileAppNamespace;
import com.thx.module.file.model.FilePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilePolicyServiceImplTest {

    @Mock
    private FileAppNamespaceMapper fileAppNamespaceMapper;
    @Mock
    private FilePolicyMapper filePolicyMapper;

    private FilePolicyServiceImpl filePolicyService;

    @BeforeEach
    void setUp() {
        filePolicyService = new FilePolicyServiceImpl(fileAppNamespaceMapper, filePolicyMapper);
    }

    @Test
    void unknownNamespaceRejected() {
        when(fileAppNamespaceMapper.selectOne(any())).thenReturn(null);
        FileSystemException ex = assertThrows(FileSystemException.class,
                () -> filePolicyService.getPolicy("cms", "unknown-namespace"));
        assertEquals(400, ex.getHttpStatus());
        assertEquals("NAMESPACE_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void disabledPolicyRejected() {
        FileAppNamespace ns = new FileAppNamespace().setAppId("cms").setNamespace("attachment")
                .setPolicyCode("PRIVATE_FILE").setStatus(1);
        when(fileAppNamespaceMapper.selectOne(any())).thenReturn(ns);
        FilePolicy policy = new FilePolicy().setPolicyCode("PRIVATE_FILE").setStatus(0);
        when(filePolicyMapper.selectOne(any())).thenReturn(policy);

        FileSystemException ex = assertThrows(FileSystemException.class,
                () -> filePolicyService.getPolicy("cms", "attachment"));
        assertEquals(400, ex.getHttpStatus());
        assertEquals("POLICY_DISABLED", ex.getErrorCode());
    }

    @Test
    void validNamespaceReturnsPolicy() {
        FileAppNamespace ns = new FileAppNamespace().setAppId("cms").setNamespace("attachment")
                .setPolicyCode("PRIVATE_FILE").setStatus(1);
        when(fileAppNamespaceMapper.selectOne(any())).thenReturn(ns);
        FilePolicy policy = new FilePolicy().setPolicyCode("PRIVATE_FILE").setStatus(1);
        when(filePolicyMapper.selectOne(any())).thenReturn(policy);

        FilePolicy result = filePolicyService.getPolicy("cms", "attachment");
        assertEquals("PRIVATE_FILE", result.getPolicyCode());
    }

    @Test
    void fileTooLargeRejected() {
        FilePolicy policy = new FilePolicy().setMaxFileSize(10L).setAccessLevel("PUBLIC");
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[100]);
        FileSystemException ex = assertThrows(FileSystemException.class, () -> filePolicyService.validate(file, policy));
        assertEquals(413, ex.getHttpStatus());
    }

    @Test
    void disallowedExtensionRejected() {
        FilePolicy policy = new FilePolicy().setMaxFileSize(1000L).setAllowedExtensions("png").setAccessLevel("PUBLIC");
        MockMultipartFile file = new MockMultipartFile("file", "a.exe", "application/octet-stream", new byte[10]);
        FileSystemException ex = assertThrows(FileSystemException.class, () -> filePolicyService.validate(file, policy));
        assertEquals(415, ex.getHttpStatus());
        assertEquals("EXTENSION_NOT_ALLOWED", ex.getErrorCode());
    }

    @Test
    void declaredContentTypeMismatchRejected() {
        FilePolicy policy = new FilePolicy().setMaxFileSize(1000L).setAllowedExtensions("jpg")
                .setAllowedMimeTypes("image/jpeg").setAccessLevel("PUBLIC");
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "text/plain", "hello".getBytes());
        FileSystemException ex = assertThrows(FileSystemException.class, () -> filePolicyService.validate(file, policy));
        assertEquals(415, ex.getHttpStatus());
        assertEquals("CONTENT_TYPE_NOT_ALLOWED", ex.getErrorCode());
    }

    @Test
    void realFileTypeMismatchRejectedByTikaDetection() {
        // 扩展名和声明的 Content-Type 都伪装成 jpg/image-jpeg，但实际内容是纯文本
        FilePolicy policy = new FilePolicy().setMaxFileSize(1000L).setAllowedExtensions("jpg")
                .setAllowedMimeTypes("image/jpeg").setAccessLevel("PUBLIC");
        MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg",
                "this is not really a jpeg, just plain text content".getBytes());
        FileSystemException ex = assertThrows(FileSystemException.class, () -> filePolicyService.validate(file, policy));
        assertEquals(415, ex.getHttpStatus());
        assertEquals("DETECTED_MIME_NOT_ALLOWED", ex.getErrorCode());
    }

    @Test
    void validFileAccepted() {
        // 最小合法 PNG 文件头（8 字节 PNG 签名）
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        FilePolicy policy = new FilePolicy().setMaxFileSize(1000L).setAllowedExtensions("png")
                .setAllowedMimeTypes("image/png").setAccessLevel("PUBLIC");
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", pngHeader);
        assertDoesNotThrow(() -> filePolicyService.validate(file, policy));
    }

    @Test
    void unknownAccessLevelConfigurationRejected() {
        FilePolicy policy = new FilePolicy().setMaxFileSize(1000L).setAccessLevel("NOT_A_REAL_LEVEL");
        MockMultipartFile file = new MockMultipartFile("file", "a.bin", "application/octet-stream", new byte[10]);
        FileSystemException ex = assertThrows(FileSystemException.class, () -> filePolicyService.validate(file, policy));
        assertEquals(403, ex.getHttpStatus());
    }
}
