package com.thx.module.file.enums;

/**
 * 文件系统调用方类型
 */
public enum CallerType {

    /** 通过 API Key 调用的外部应用 */
    APPLICATION,

    /** 代表具体终端用户的调用 */
    USER,

    /** 同一 Spring Boot 进程内的内部模块调用（如 CMS 自身） */
    SYSTEM
}
