package com.thx.module.admin.service;

import com.thx.module.admin.mapper.BizArticleMapper;
import com.thx.module.file.model.FileAsset;
import com.thx.module.file.service.FileCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文章图片清理服务
 * 负责扫描并逻辑删除未被任何文章引用的孤立图片文件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleImageCleanupService {

    private final BizArticleMapper bizArticleMapper;
    private final FileCleanupService fileCleanupService;

    /**
     * 扫描并逻辑删除未被任何文章引用的孤立图片文件
     */
    public void cleanupOrphanArticleImages() {
        List<FileAsset> orphanFiles = bizArticleMapper.findOrphanArticleImages();
        if (orphanFiles.isEmpty()) {
            log.debug("未发现孤立的文章图片");
            return;
        }

        log.info("发现 {} 个未被文章引用的孤立图片，开始逻辑删除", orphanFiles.size());
        int deletedCount = 0;
        for (FileAsset asset : orphanFiles) {
            try {
                fileCleanupService.softDelete(asset);
                deletedCount++;
                log.info("逻辑删除孤立图片: fileId={}, originalName={}", asset.getFileId(), asset.getOriginalName());
            } catch (Exception e) {
                log.error("逻辑删除孤立图片失败: fileId={}", asset.getFileId(), e);
            }
        }
        log.info("孤立图片清理完成: 共扫描 {} 个，成功删除 {} 个", orphanFiles.size(), deletedCount);
    }
}
