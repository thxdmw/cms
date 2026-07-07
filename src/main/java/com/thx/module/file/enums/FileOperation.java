package com.thx.module.file.enums;

/**
 * 文件操作类型，用于审计日志记录
 */
public enum FileOperation {

    /** 上传文件 */
    UPLOAD,

    /** 查询文件元数据 */
    READ,

    /** 分页查询文件列表 */
    LIST,

    /** 生成下载/预签名 URL */
    PRESIGN,

    /** 逻辑删除文件 */
    DELETE,

    /** 物理清理（真正从对象存储删除） */
    PURGE
}
