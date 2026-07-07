package com.thx.exception;

/**
 * 自定义上传文件未找到异常
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
public class UploadFileException extends RuntimeException {

    private static final long serialVersionUID = -7812372861340782170L;

    public UploadFileException() {
    }

    public UploadFileException(String message) {
        super(message);
    }

}
