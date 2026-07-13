package com.thx.module.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.enums.FileAccessLevel;
import com.thx.module.file.enums.FileStatus;
import com.thx.module.file.mapper.FileAssetMapper;
import com.thx.module.file.model.FileAsset;
import com.thx.module.file.service.FileAuthService;
import com.thx.module.file.service.FileObjectLookupService;
import com.thx.module.file.vo.FileInfoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** module.file 内部内容哈希查询实现。 */
@Service
@RequiredArgsConstructor
public class FileObjectLookupServiceImpl implements FileObjectLookupService {

    private final FileAssetMapper fileAssetMapper;
    private final FileAuthService fileAuthService;

    @Override
    public FileInfoResult findActiveByHash(String namespace,
                                           String sha256,
                                           long size,
                                           FileCallerContext caller) {
        LambdaQueryWrapper<FileAsset> wrapper = new LambdaQueryWrapper<FileAsset>()
                .eq(FileAsset::getAppId, caller.getAppId())
                .eq(FileAsset::getNamespace, namespace)
                .eq(FileAsset::getSha256, sha256)
                .eq(FileAsset::getSize, size)
                .eq(FileAsset::getStatus, FileStatus.ACTIVE.name());

        // Hash 不是权限边界。同 App 下不同用户可能拥有相同内容，查询阶段必须排除
        // 当前 Caller 不可访问的 OWNER_ONLY 资产，否则 LIMIT 1 可能误命中其他用户并返回 403。
        if (caller.getUserId() == null) {
            wrapper.ne(FileAsset::getAccessLevel, FileAccessLevel.OWNER_ONLY.name());
        } else {
            wrapper.and(accessible -> accessible
                    .ne(FileAsset::getAccessLevel, FileAccessLevel.OWNER_ONLY.name())
                    .or()
                    .eq(FileAsset::getOwnerId, caller.getUserId()));
        }

        FileAsset asset = fileAssetMapper.selectOne(wrapper.last("LIMIT 1"));
        if (asset == null) {
            return null;
        }
        fileAuthService.checkRead(caller, asset);
        return new FileInfoResult(
                asset.getFileId(),
                asset.getNamespace(),
                asset.getOriginalName(),
                asset.getExtension(),
                asset.getContentType(),
                asset.getSize(),
                asset.getSha256(),
                asset.getAccessLevel(),
                asset.getOwnerId(),
                asset.getStatus(),
                asset.getCreateTime());
    }
}
