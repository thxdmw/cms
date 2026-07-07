package com.thx.module.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Data
public class UploadResponse {

    private String fileId;
    private String fileName;
    private Long size;
    private String type;
    private String url;
    private Integer status;
    private String msg;

    private UploadResponse(String fileName, String type, String url, Integer status) {
        this.fileName = fileName;
        this.type = type;
        this.url = url;
        this.status = status;
    }

    private UploadResponse(String fileId, String fileName, String type, String url, Integer status) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.type = type;
        this.url = url;
        this.status = status;
    }

    private UploadResponse(Integer status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    public static UploadResponse success(String fileName, String type, String url, Integer status) {
        return new UploadResponse(fileName, type, url, status);
    }

    public static UploadResponse success(String fileId, String fileName, String type, String url, Integer status) {
        return new UploadResponse(fileId, fileName, type, url, status);
    }

    public static UploadResponse failed(Integer status, String msg) {
        return new UploadResponse(status, msg);
    }

    @AllArgsConstructor
    public enum ErrorEnum {
        //无
        NONE("None"),
        // 文件超出大小限制
        OVER_SIZE("文件超出大小限制"),
        // 文件格式不合规范
        ILLEGAL_EXTENSION("文件格式不合规范"),
        // 文件不存在
        FILE_NOT_FOUND("文件不存在");

        public final String msg;

    }
}
