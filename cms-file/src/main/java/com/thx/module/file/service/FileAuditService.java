package com.thx.module.file.service;

import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.enums.FileOperation;

/**
 * 文件操作审计服务
 * 审计失败不能影响文件主业务，实现内部必须自行吞掉异常
 */
public interface FileAuditService {

    /**
     * 记录一条操作审计日志
     * @param caller    调用方上下文
     * @param fileId    文件标识，可能为空（如上传失败时）
     * @param operation 操作类型
     * @param result    SUCCESS / FAIL
     * @param errorCode 失败时的错误码，成功可为空
     * @param ip        客户端 IP，可能为空
     */
    void log(FileCallerContext caller, String fileId, FileOperation operation, String result, String errorCode, String ip);
}
