package com.thx.module.file.service.impl;

import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.enums.FileAccessLevel;
import com.thx.module.file.exception.FileSystemException;
import com.thx.module.file.model.FileAsset;
import com.thx.module.file.service.FileAuthService;
import org.springframework.stereotype.Service;

/**
 * FileAuthService 默认实现
 * 先做 App 隔离（跨 App 一律 404），再按 PUBLIC/APP_INTERNAL/OWNER_ONLY 三种
 * 访问级别做简单 switch 判断，未知级别默认拒绝，暂不引入 Strategy 抽象
 */
@Service
public class FileAuthServiceImpl implements FileAuthService {

    @Override
    public void checkRead(FileCallerContext caller, FileAsset file) {
        checkAccess(caller, file);
    }

    @Override
    public void checkDelete(FileCallerContext caller, FileAsset file) {
        checkAccess(caller, file);
    }

    /** App 隔离 + 按访问级别做 switch 判断的公共校验逻辑 */
    private void checkAccess(FileCallerContext caller, FileAsset file) {
        // 先做 App 隔离，跨 App 一律按 404 处理，避免泄漏跨 App 文件是否存在
        if (!caller.getAppId().equals(file.getAppId())) {
            throw FileSystemException.notFound("文件不存在");
        }

        FileAccessLevel accessLevel;
        try {
            accessLevel = FileAccessLevel.valueOf(file.getAccessLevel());
        } catch (IllegalArgumentException e) {
            // 未知 AccessLevel 默认拒绝
            throw FileSystemException.forbidden("拒绝访问");
        }

        switch (accessLevel) {
            case PUBLIC:
                return;
            case APP_INTERNAL:
                // 同 App 且拥有对应 Scope 才能到达这里，Scope 已在拦截器层校验过
                return;
            case OWNER_ONLY:
                checkOwner(caller, file);
                return;
            default:
                throw FileSystemException.forbidden("拒绝访问");
        }
    }

    /** OWNER_ONLY 的所有者匹配校验：caller.userId 必须非空且等于 file.ownerId */
    private void checkOwner(FileCallerContext caller, FileAsset file) {
        if (caller.getUserId() == null || !caller.getUserId().equals(file.getOwnerId())) {
            throw FileSystemException.forbidden("无权访问该文件");
        }
    }
}
