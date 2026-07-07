package com.thx.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 *
 * @author tanghaixin
 * @date 2020/4/18 12:10 下午
 */
@Data
@ConfigurationProperties(prefix = "file")
public class FileUploadProperties {

    public static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif",
            "bmp", "webp", "tiff", "svg", "pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "mp3", "wav", "ogg", "flac", "aac",
            "mp4", "avi", "mov", "mkv", "webm"));

    private String accessPathPattern = "/u/**";
    private String uploadFolder;
    private String accessPrefixUrl;
    /**
     * 允许上传的文件类型
     */
    private Set<String> allowedExtensions = DEFAULT_ALLOWED_EXTENSIONS;
}
