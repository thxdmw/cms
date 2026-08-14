package com.thx.module.file.enums;

/**
 * 文件访问级别
 * PUBLIC：同 App 文件 API 可读，是否真正匿名访问由 Bucket Policy 决定
 * APP_INTERNAL：同 App 且拥有 READ Scope 可访问
 * OWNER_ONLY：必须 caller.appId == file.appId 且 caller.userId == file.ownerId
 * 未知级别一律拒绝访问
 */
public enum FileAccessLevel {

    /** 公开：同 App 文件 API 均可读取，是否真正匿名访问由 Bucket Policy 决定 */
    PUBLIC,

    /** App 内部：同 App 且拥有 READ Scope 即可访问 */
    APP_INTERNAL,

    /** 仅所有者：必须同 App 且 caller.userId 等于 file.ownerId 才能访问 */
    OWNER_ONLY
}
