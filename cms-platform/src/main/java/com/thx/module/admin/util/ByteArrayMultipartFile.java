package com.thx.module.admin.util;

import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 内存字节数组包装成的 MultipartFile，供服务器文件在线编辑保存时复用
 * FileSystemService.upload() 只接受 MultipartFile，而编辑保存的内容来自前端提交的文本而非真实的 HTTP 上传，
 * 需要这样一个内存实现来适配已有接口
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2026年7月6日
 */
public class ByteArrayMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] content;

    public ByteArrayMultipartFile(String originalFilename, String contentType, byte[] content) {
        this.name = "file";
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.content = content;
    }

    public static ByteArrayMultipartFile ofText(String originalFilename, String text) {
        return new ByteArrayMultipartFile(originalFilename, "text/plain", text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    @NonNull
    public byte[] getBytes() {
        return content;
    }

    @Override
    @NonNull
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(@NonNull java.io.File dest) throws IOException {
        try (OutputStream os = new java.io.FileOutputStream(dest)) {
            os.write(content);
        }
    }
}
