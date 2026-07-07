package com.thx.module.file.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文件上传结果 VO
 */
@Data
@AllArgsConstructor
public class FileUploadResult {
    /** 文件唯一标识 */
    private String fileId;
    /** 原始文件名 */
    private String originalName;
    /** 访问 URL（PUBLIC 为公开地址，其余为一次性 Presigned URL） */
    private String url;
}
