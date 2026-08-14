package com.thx.module.file.service;

import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.model.FileAsset;

/**
 * 文件访问权限校验
 * 只有三种访问级别（PUBLIC / APP_INTERNAL / OWNER_ONLY），当前不做 Strategy 抽象
 */
public interface FileAuthService {

    /** 校验是否允许读取（GET 元数据 / 获取下载 URL），跨 App 一律按文件不存在处理 */
    void checkRead(FileCallerContext caller, FileAsset file);

    /** 校验是否允许删除，规则与 checkRead 相同 */
    void checkDelete(FileCallerContext caller, FileAsset file);
}
