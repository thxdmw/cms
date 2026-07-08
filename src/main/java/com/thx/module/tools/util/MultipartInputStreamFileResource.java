package com.thx.module.tools.util;

import org.springframework.core.io.InputStreamResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 把 Spring {@link MultipartFile} 包装成可用于 {@code RestTemplate} 转发的 {@link InputStreamResource}。
 * <p>
 * 直接用 {@code new InputStreamResource(multipartFile.getInputStream())} 时 {@link #getFilename()}
 * 会返回 null，导致用 {@code RestTemplate} 把文件转发给外部服务（拼 multipart/form-data 请求体）时对方拿不到文件名，
 * 这里重写 {@link #getFilename()} 补上原始文件名。
 * 用于 {@link com.thx.module.tools.service.impl.PdfConvertServiceImpl} 把用户上传的 PDF/图片转发给外部 Python 转换服务。
 */
public class MultipartInputStreamFileResource extends InputStreamResource {

    private final String filename;

    public MultipartInputStreamFileResource(MultipartFile multipartFile) throws IOException {
        super(multipartFile.getInputStream());
        this.filename = multipartFile.getOriginalFilename();
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    public long contentLength() {
        // 长度未知：底层是流式的 InputStream，无法在不消费的前提下预先得知长度，
        // 返回 -1 让上层（HttpClient）按 chunked 方式传输。
        return -1;
    }
}
