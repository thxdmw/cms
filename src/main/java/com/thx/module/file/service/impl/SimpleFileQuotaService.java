package com.thx.module.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.file.exception.FileSystemException;
import com.thx.module.file.mapper.FileAppMapper;
import com.thx.module.file.mapper.FileAssetMapper;
import com.thx.module.file.model.FileApp;
import com.thx.module.file.service.FileQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 基于 SQL SUM 的配额校验实现
 * 未设置 quota_bytes 表示不限制
 */
@Service
@RequiredArgsConstructor
public class SimpleFileQuotaService implements FileQuotaService {

    /** 查询 App 配置的配额上限（quota_bytes） */
    private final FileAppMapper fileAppMapper;
    /** 统计 App 当前已使用的字节数 */
    private final FileAssetMapper fileAssetMapper;

    @Override
    public void checkUploadAllowed(String appId, long uploadSize) {
        FileApp app = fileAppMapper.selectOne(
                new LambdaQueryWrapper<FileApp>().eq(FileApp::getAppId, appId));
        if (app == null || app.getQuotaBytes() == null) {
            return;
        }
        long used = fileAssetMapper.sumActiveSize(appId);
        if (used + uploadSize > app.getQuotaBytes()) {
            throw new FileSystemException(403, "QUOTA_EXCEEDED", "存储配额不足");
        }
    }
}
