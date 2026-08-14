package com.thx.module.file.util;

import cn.hutool.core.util.StrUtil;
import lombok.experimental.UtilityClass;

/**
 * URL 拼接工具类
 * 自动处理斜杠重复问题
 */
@UtilityClass
public class UrlJoinUtil {

    /**
     * 拼接 URL 各部分，自动处理斜杠
     * @param parts URL 的各部分
     * @return 拼接后的完整 URL
     */
    public static String join(String... parts) {
        if (parts == null || parts.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (StrUtil.isBlank(part)) {
                continue;
            }

            if (i == 0) {
                // 第一段：去掉末尾斜杠
                sb.append(part.replaceAll("/+$", ""));
            } else {
                // 后续段落：去掉前导斜杠后，先添加分隔符再拼接
                sb.append("/").append(part.replaceAll("^/+", ""));
            }
        }

        return sb.toString();
    }
}
