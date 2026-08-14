package com.thx.module.file.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 对象存储清理补偿任务
 * 对应表：storage_cleanup_task
 * taskType 取值：DELETE_OBJECT（删除单个对象）/ CLEAN_ORPHAN（清理上传失败产生的孤儿对象）
 * status 取值：PENDING / PROCESSING / SUCCESS / FAILED
 */
@Data
@Accessors(chain = true)
@TableName("storage_cleanup_task")
public class StorageCleanupTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的文件标识，CLEAN_ORPHAN 场景下可能为空（DB 记录都没写成功） */
    private String fileId;

    /** 目标对象所在的桶 */
    private String bucket;

    /** 目标对象键 */
    private String objectKey;

    /** 任务类型：DELETE_OBJECT / CLEAN_ORPHAN */
    private String taskType;

    /** 任务状态：PENDING / PROCESSING / SUCCESS / FAILED */
    private String status;

    /** 已重试次数，达到最大重试次数后置为 FAILED，等待人工处理 */
    private Integer retryCount;

    /** 下次允许重试的时间，按退避策略递增（1m/5m/30m/2h/6h/24h） */
    private Date nextRetryTime;

    /** 最近一次失败的错误信息（已截断），便于排查 */
    private String lastError;

    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
