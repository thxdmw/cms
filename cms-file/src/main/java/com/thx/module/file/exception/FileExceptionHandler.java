package com.thx.module.file.exception;

import com.thx.module.file.vo.ResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 文件系统统一异常处理
 * 只作用于 com.thx.module.file.controller 包下的接口，返回正确的 HTTP 状态码，
 * 不把 MinIO / 数据库等底层异常原文返回给客户端
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.thx.module.file.controller")
public class FileExceptionHandler {

    /** 处理业务异常：按异常自带的 httpStatus 返回，5xx 记 error 日志，其余记 warn */
    @ExceptionHandler(FileSystemException.class)
    public ResponseEntity<ResponseVo<Void>> handleFileSystemException(FileSystemException e) {
        if (e.getHttpStatus() >= 500) {
            log.error("文件系统异常: errorCode={}, message={}", e.getErrorCode(), e.getMessage(), e);
        } else {
            log.warn("文件系统请求被拒绝: errorCode={}, message={}", e.getErrorCode(), e.getMessage());
        }
        return ResponseEntity.status(e.getHttpStatus())
                .body(ResponseVo.error(e.getHttpStatus(), e.getMessage()));
    }

    /** Spring MVC 层面的请求体过大异常（在 FilePolicyService 校验之前就可能触发） */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResponseVo<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(413).body(ResponseVo.error(413, "文件超出上传大小限制"));
    }

    /** 兜底处理未预期的异常，只返回通用文案，避免把底层异常堆栈暴露给客户端 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseVo<Void>> handleUnknown(Exception e) {
        log.error("文件系统未预期异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body(ResponseVo.error(500, "服务器内部错误"));
    }
}
