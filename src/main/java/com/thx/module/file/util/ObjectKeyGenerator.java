package com.thx.module.file.util;

import com.thx.module.file.exception.FileSystemException;
import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * 对象键（Object Key）生成工具
 * 格式：apps/{appId}/{namespace}/{yyyy}/{MM}/{dd}/{fileId}.{extension}
 * fileId 由业务层在上传前生成，不使用 originalName，防止路径穿越
 */
@UtilityClass
public class ObjectKeyGenerator {

    /** appId / fileId 允许的字符集：大小写字母、数字、下划线、短横线 */
    private static final Pattern APP_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    /** namespace 允许的字符集：小写字母、数字、下划线、短横线 */
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("^[a-z0-9_-]+$");
    /** 用于从扩展名中剔除非字母数字字符（含 . 和 / 之类的路径穿越字符） */
    private static final Pattern EXTENSION_PATTERN = Pattern.compile("[^a-zA-Z0-9]");

    /**
     * 生成对象键：apps/{appId}/{namespace}/{yyyy}/{MM}/{dd}/{fileId}.{extension}
     * @param appId     调用方 App 标识，只允许 [a-zA-Z0-9_-]
     * @param namespace 业务场景，只允许 [a-z0-9_-]
     * @param fileId    业务层预先生成的文件唯一标识，只允许 [a-zA-Z0-9_-]
     * @param extension 文件扩展名，会被剔除非字母数字字符后再拼接，可为空
     */
    public static String generate(String appId, String namespace, String fileId, String extension) {
        if (appId == null || !APP_ID_PATTERN.matcher(appId).matches()) {
            throw FileSystemException.badRequest("INVALID_APP_ID", "非法的 appId");
        }
        if (namespace == null || !NAMESPACE_PATTERN.matcher(namespace).matches()) {
            throw FileSystemException.badRequest("INVALID_NAMESPACE", "非法的 namespace");
        }
        if (fileId == null || !APP_ID_PATTERN.matcher(fileId).matches()) {
            throw FileSystemException.badRequest("INVALID_FILE_ID", "非法的 fileId");
        }

        String datePart = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String safeExt = extension == null ? "" : EXTENSION_PATTERN.matcher(extension).replaceAll("");

        StringBuilder objectKey = new StringBuilder("apps/")
                .append(appId).append("/")
                .append(namespace).append("/")
                .append(datePart).append("/")
                .append(fileId);
        if (!safeExt.isEmpty()) {
            objectKey.append(".").append(safeExt);
        }
        return objectKey.toString();
    }
}
