package com.thx.module.file.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文件 URL 结果 VO
 * 返回文件标识和对应的访问 URL
 */
@Data
@AllArgsConstructor
public class FileUrlResult {
    /** 文件唯一标识 */
    private String fileId;
    /** 文件访问 URL */
    private String url;
}
