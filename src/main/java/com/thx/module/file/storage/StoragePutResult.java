package com.thx.module.file.storage;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 对象存储写入结果
 * 只包含存储层关心的信息，不认识 appId / namespace / FilePolicy 等业务概念
 */
@Data
@AllArgsConstructor
public class StoragePutResult {

    /** 实际写入的存储桶 */
    private String bucket;

    /** 实际写入的对象键 */
    private String objectKey;

    /** 对象存储返回的 ETag，注意 ETag 不等于 SHA256 */
    private String etag;
}
