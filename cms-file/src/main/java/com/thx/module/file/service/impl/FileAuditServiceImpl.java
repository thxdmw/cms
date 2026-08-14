package com.thx.module.file.service.impl;

import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.enums.FileOperation;
import com.thx.module.file.mapper.FileOperationLogMapper;
import com.thx.module.file.model.FileOperationLog;
import com.thx.module.file.service.FileAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 审计日志实现
 * 不记录 API Key / MinIO Secret / Authorization / 完整 Presigned URL，
 * 任何异常都在内部吞掉，不能影响主业务流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileAuditServiceImpl implements FileAuditService {

    /** 写入 file_operation_log 表 */
    private final FileOperationLogMapper fileOperationLogMapper;

    @Override
    public void log(FileCallerContext caller, String fileId, FileOperation operation, String result, String errorCode, String ip) {
        try {
            FileOperationLog entry = new FileOperationLog()
                    .setAppId(caller.getAppId())
                    .setUserId(caller.getUserId())
                    .setFileId(fileId)
                    .setOperation(operation.name())
                    .setResult(result)
                    .setRequestId(caller.getRequestId())
                    .setIp(ip)
                    .setErrorCode(errorCode);
            fileOperationLogMapper.insert(entry);
        } catch (Exception e) {
            log.error("记录文件操作审计日志失败: operation={}, fileId={}", operation, fileId, e);
        }
    }
}
