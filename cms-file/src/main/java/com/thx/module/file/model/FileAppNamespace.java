package com.thx.module.file.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * App 业务场景（namespace）到 FilePolicy 的映射
 * 对应表：file_app_namespace，如 (cms, article-image) -&gt; PUBLIC_IMAGE
 */
@Data
@Accessors(chain = true)
@TableName("file_app_namespace")
public class FileAppNamespace implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属应用标识，对应 file_app.app_id */
    private String appId;

    /** 业务场景标识，如 article-image、attachment、save，App 内唯一，建议小写 */
    private String namespace;

    /** 使用的策略编码，对应 file_policy.policy_code */
    private String policyCode;

    /** 1-启用，0-禁用；禁用后该 namespace 的上传一律拒绝 */
    private Integer status;

    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
}
