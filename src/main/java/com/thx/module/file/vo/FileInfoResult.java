package com.thx.module.file.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

/**
 * 文件元数据查询结果 VO
 * 不包含 bucket / objectKey 等存储内部细节
 */
@Data
@AllArgsConstructor
public class FileInfoResult {
    /** 文件唯一标识 */
    private String fileId;
    /** 所属业务场景 */
    private String namespace;
    /** 原始文件名 */
    private String originalName;
    /** 文件扩展名 */
    private String extension;
    /** 客户端声明的 Content-Type */
    private String contentType;
    /** 文件大小（字节） */
    private Long size;
    /** 文件内容 SHA256 */
    private String sha256;
    /** 访问级别：PUBLIC / APP_INTERNAL / OWNER_ONLY */
    private String accessLevel;
    /** 所有者用户 ID，可能为空 */
    private String ownerId;
    /** 文件状态：ACTIVE/DELETED/PURGING/PURGED/PURGE_FAILED */
    private String status;
    /** 上传时间 */
    private Date createTime;
}
