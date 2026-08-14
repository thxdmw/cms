package com.thx.module.file.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件操作审计日志
 * 对应表：file_operation_log
 * 禁止记录 API Key / MinIO Secret / Authorization / 完整 Presigned URL，
 * 写入失败不能影响文件主业务（由 FileAuditServiceImpl 内部吞掉异常）
 */
@Data
@Accessors(chain = true)
@TableName("file_operation_log")
public class FileOperationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 调用方 App 标识 */
    private String appId;

    /** 调用方用户 ID，可能为空（如 APPLICATION 类型调用） */
    private String userId;

    /** 关联的文件标识，可能为空（如上传失败、LIST 操作） */
    private String fileId;

    /** 对应 FileOperation 枚举名：UPLOAD/READ/LIST/PRESIGN/DELETE/PURGE */
    private String operation;

    /** 操作结果：SUCCESS / FAIL */
    private String result;

    /** 请求追踪 ID，用于关联同一次请求的多条日志 */
    private String requestId;

    /** 客户端 IP */
    private String ip;

    /** 失败时的错误码，成功时可为空 */
    private String errorCode;

    /** 创建时间 */
    private Date createTime;
}
