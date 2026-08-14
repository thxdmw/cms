package com.thx.module.file.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 文件策略：大小、扩展名、MIME、访问级别、Bucket 等纯数据配置
 * 对应表：file_policy，只保存数据配置，不支持 SpEL/脚本等动态规则
 */
@Data
@Accessors(chain = true)
@TableName("file_policy")
public class FilePolicy implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 策略编码，如 PUBLIC_IMAGE、PRIVATE_FILE、GAME_SAVE，全局唯一 */
    private String policyCode;

    /** 允许的最大文件大小（字节） */
    private Long maxFileSize;

    /** 逗号分隔的扩展名，如 jpg,jpeg,png,webp；为空表示不限制 */
    private String allowedExtensions;

    /** 逗号分隔的 MIME 类型，如 image/jpeg,image/png；为空表示不限制 */
    private String allowedMimeTypes;

    /** 对应 FileAccessLevel 枚举名：PUBLIC / APP_INTERNAL / OWNER_ONLY */
    private String accessLevel;

    /** 文件实际存储的 MinIO 桶名称 */
    private String bucket;

    /** 1-要求校验 SHA256，0-不要求 */
    private Integer checksumRequired;

    /** 1-启用，0-禁用；禁用后引用该策略的 namespace 一律拒绝上传 */
    private Integer status;

    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;

    /** 将逗号分隔的 allowedExtensions 解析为小写集合，不持久化 */
    public Set<String> allowedExtensionSet() {
        return splitCsv(allowedExtensions);
    }

    /** 将逗号分隔的 allowedMimeTypes 解析为小写集合，不持久化 */
    public Set<String> allowedMimeTypeSet() {
        return splitCsv(allowedMimeTypes);
    }

    /** 把逗号分隔字符串解析为去空格、去空项、转小写的集合 */
    private Set<String> splitCsv(String csv) {
        Set<String> result = new LinkedHashSet<>();
        if (csv == null || csv.trim().isEmpty()) {
            return result;
        }
        Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .forEach(result::add);
        return result;
    }
}
