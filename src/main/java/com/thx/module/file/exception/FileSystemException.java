package com.thx.module.file.exception;

import lombok.Getter;

/**
 * 文件系统业务异常
 * 携带 errorCode 与 httpStatus，由 FileExceptionHandler 统一转换为 HTTP 响应，
 * message 必须是可以直接返回给客户端的安全文案，不能包含底层异常细节
 */
@Getter
public class FileSystemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 返回给客户端的 HTTP 状态码，如 400/403/404/413/415/422/500 */
    private final int httpStatus;

    /** 业务错误码，便于客户端按代码分支处理（而不是解析文案） */
    private final String errorCode;

    public FileSystemException(int httpStatus, String errorCode, String safeMessage) {
        super(safeMessage);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public static FileSystemException notFound(String message) {
        return new FileSystemException(404, "FILE_NOT_FOUND", message);
    }

    public static FileSystemException forbidden(String message) {
        return new FileSystemException(403, "ACCESS_DENIED", message);
    }

    public static FileSystemException badRequest(String errorCode, String message) {
        return new FileSystemException(400, errorCode, message);
    }
}
