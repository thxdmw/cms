package com.thx.module.file.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件资产元数据
 * 对应表：file_asset，只保存 storage_provider / bucket / object_key，
 * 不保存 url / presigned_url，访问 URL 由 FileUrlService 动态生成
 */
@Data
@Accessors(chain = true)
@TableName("file_asset")
public class FileAsset implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文件唯一标识（UUID，去掉横线的 32 位十六进制），对外暴露的文件 ID */
    private String fileId;

    /** 所属应用标识，只能来自 FileCallerContext，禁止从请求参数接受 */
    private String appId;

    /** 上传时使用的业务场景（namespace） */
    private String namespace;

    /** 上传时命中的文件策略编码，对应 file_policy.policy_code */
    private String policyCode;

    /** 原始文件名（用户上传时的文件名，不用于生成对象键，防止路径穿越） */
    private String originalName;

    /** 文件扩展名（不含点），如 jpg */
    private String extension;

    /** 客户端声明的 Content-Type，不完全可信 */
    private String contentType;

    /** Apache Tika 检测出的真实 MIME 类型，用于识别伪造扩展名/Content-Type 的文件 */
    private String detectedMimeType;

    /** 文件大小（字节） */
    private Long size;

    /** 文件内容的 SHA256（十六进制），上传时通过 DigestInputStream 一次读取计算得到 */
    private String sha256;

    /** 存储提供方，目前固定为 MINIO，为未来接入 S3/阿里云 OSS 等预留 */
    private String storageProvider;

    /** 实际存储的 MinIO 桶名称 */
    private String bucket;

    /** 对象存储中的对象键，格式 apps/{appId}/{namespace}/{yyyy}/{MM}/{dd}/{fileId}.{ext} */
    private String objectKey;

    /** 对象存储返回的 ETag，注意 ETag != SHA256，两者语义和计算方式都不同 */
    private String etag;

    /** 对应 FileAccessLevel 枚举名：PUBLIC / APP_INTERNAL / OWNER_ONLY */
    private String accessLevel;

    /** 所有者类型，当前固定为 USER（有 ownerId 时），为空表示无所有者 */
    private String ownerType;

    /** 所有者用户 ID，OWNER_ONLY 策略下必须非空 */
    private String ownerId;

    /** 对应 FileStatus 枚举名：ACTIVE/DELETED/PURGING/PURGED/PURGE_FAILED */
    private String status;

    /** 逻辑删除时间，用于计算物理清理的宽限期 */
    private Date deletedAt;

    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
