package com.thx.module.file.enums;

/**
 * 文件生命周期状态
 */
public enum FileStatus {

    /** 有效 */
    ACTIVE,

    /** 已逻辑删除，等待宽限期后物理清理 */
    DELETED,

    /** 正在物理清理中（CAS 占用状态，防止并发重复清理） */
    PURGING,

    /** 已物理清理完成 */
    PURGED,

    /** 物理清理失败，等待重试或人工处理 */
    PURGE_FAILED
}
