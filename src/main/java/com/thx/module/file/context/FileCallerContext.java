package com.thx.module.file.context;

import com.thx.module.file.enums.CallerType;
import lombok.Data;

import java.util.Set;

/**
 * 文件系统调用方上下文
 * FileSystemService 及以下各层只能依赖该对象，禁止直接读取
 * HttpServletRequest / Header / Shiro Subject
 */
@Data
public class FileCallerContext {

    /** 调用方所属的 App 标识，对应 file_app.app_id，文件的 app_id 始终取自这里，不接受请求参数传入 */
    private String appId;

    /** 调用方对应的终端用户 ID；APPLICATION 类型调用可能为空，OWNER_ONLY 文件权限校验依赖该字段 */
    private String userId;

    /** 调用方类型：APPLICATION（外部应用 API Key 调用）/ USER / SYSTEM（CMS 等内部模块调用） */
    private CallerType callerType;

    /** 调用方被授予的 Scope 集合，如 UPLOAD/READ/DELETE/LIST/PRESIGN */
    private Set<String> scopes;

    /** 请求追踪 ID，用于审计日志关联同一次请求 */
    private String requestId;

    /** 客户端 IP，仅用于审计日志，由构造上下文的一方（Controller/拦截器）填充 */
    private String ip;
}
