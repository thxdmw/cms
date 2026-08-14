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
 * 使用文件系统的应用（CMS / Pet App / Game / Agent 等）
 * 对应表：file_app
 */
@Data
@Accessors(chain = true)
@TableName("file_app")
public class FileApp implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 应用标识，如 cms、pet-app、game-a，全局唯一 */
    private String appId;

    /** 应用名称，便于人工识别 */
    private String appName;

    /** API Key 的 SHA-256 哈希，禁止保存明文 */
    private String apiKeyHash;

    /** 逗号分隔的 Scope 列表，如 UPLOAD,READ,DELETE,LIST,PRESIGN */
    private String scopes;

    /** 存储配额（字节），为空表示不限制 */
    private Long quotaBytes;

    /** 1-启用，0-禁用；禁用后该 App 的所有请求一律拒绝 */
    private Integer status;

    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;

    /** 将逗号分隔的 scopes 解析为集合，不持久化 */
    public Set<String> scopeSet() {
        Set<String> result = new LinkedHashSet<>();
        if (scopes == null || scopes.trim().isEmpty()) {
            return result;
        }
        Arrays.stream(scopes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(result::add);
        return result;
    }
}
