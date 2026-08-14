package com.thx.module.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文件上传接口的响应体（{@link com.thx.module.admin.controller.file.UploadController}
 * 使用），success/failed 两个静态工厂方法分别对应成功和失败两种场景需要的字段组合。
 */
@Data
public class UploadResponse {

    /** 文件系统里的文件 id（对应 file 模块的 file_id） */
    private String fileId;
    /** 文件名 */
    private String fileName;
    /** 文件大小（当前未在任何工厂方法里赋值，预留字段） */
    private Long size;
    /** 文件扩展名/类型 */
    private String type;
    /** 文件访问地址 */
    private String url;
    /** 响应状态码 */
    private Integer status;
    /** 失败时的提示信息 */
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
