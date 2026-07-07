package com.thx.module.admin.service.impl;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thx.common.util.Pagination;
import com.thx.module.admin.entity.BizServerFile;
import com.thx.module.admin.mapper.BizServerFileMapper;
import com.thx.module.admin.service.BizServerFileService;
import com.thx.module.admin.util.ByteArrayMultipartFile;
import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.service.FileSystemService;
import com.thx.module.file.vo.FileUploadResult;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author tanghaixin
 * @version V1.0
 * @date 2026年7月6日
 */
@Service
@AllArgsConstructor
public class BizServerFileServiceImpl extends ServiceImpl<BizServerFileMapper, BizServerFile> implements BizServerFileService {

    /** file 模块的业务场景标识，对应 file_app_namespace(app_id='cms', namespace='server-file') */
    private static final String NAMESPACE = "server-file";

    /** 可在线预览/编辑的文本类扩展名（小写，不含点） */
    private static final Set<String> EDITABLE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "txt", "md", "markdown", "log", "conf", "cfg", "ini", "properties",
            "yml", "yaml", "json", "xml", "sql", "sh", "bat", "ps1", "gitignore", "env",
            "js", "mjs", "ts", "jsx", "tsx", "css", "html", "htm", "java", "py",
            "c", "cpp", "h", "hpp", "go", "rs", "rb", "php", "vue", "csv"));

    /** 超过此大小即使是文本扩展名也不提供在线预览/编辑（避免大文件卡顿）：2MB */
    private static final long EDITABLE_MAX_SIZE = 2 * 1024 * 1024L;

    private final FileSystemService fileSystemService;

    @Override
    public IPage<BizServerFile> pageServerFiles(String keyword, Integer pageNumber, Integer pageSize) {
        IPage<BizServerFile> page = new Pagination<>(pageNumber, pageSize);
        LambdaQueryWrapper<BizServerFile> wrapper = new LambdaQueryWrapper<BizServerFile>()
                .like(StrUtil.isNotBlank(keyword), BizServerFile::getOriginalName, keyword)
                .orderByDesc(BizServerFile::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizServerFile uploadFile(MultipartFile file, String remark, String uploader, FileCallerContext caller) {
        FileUploadResult result = fileSystemService.upload(file, NAMESPACE, caller.getUserId(), caller);
        String extension = FileNameUtil.getSuffix(result.getOriginalName()).toLowerCase();
        long size = file.getSize();

        BizServerFile entity = new BizServerFile()
                .setFileId(result.getFileId())
                .setOriginalName(result.getOriginalName())
                .setExtension(extension)
                .setContentType(file.getContentType())
                .setSize(size)
                .setEditable(isEditable(extension, size) ? 1 : 0)
                .setUploader(uploader)
                .setUploadUserId(caller.getUserId())
                .setRemark(remark);
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        this.save(entity);
        return entity;
    }

    @Override
    public String getDownloadUrl(String id, FileCallerContext caller) {
        BizServerFile record = getValidById(id);
        return fileSystemService.getDownloadUrl(record.getFileId(), caller);
    }

    @Override
    public String previewContent(String id, FileCallerContext caller) {
        BizServerFile record = getValidById(id);
        if (record.getEditable() == null || record.getEditable() != 1) {
            throw new IllegalArgumentException("该文件不支持在线预览");
        }
        String url = fileSystemService.getDownloadUrl(record.getFileId(), caller);
        return HttpUtil.get(url);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveContent(String id, String content, FileCallerContext caller) {
        BizServerFile record = getValidById(id);
        if (record.getEditable() == null || record.getEditable() != 1) {
            throw new IllegalArgumentException("该文件不支持在线编辑");
        }
        String oldFileId = record.getFileId();
        MultipartFile newFile = ByteArrayMultipartFile.ofText(record.getOriginalName(), content);
        FileUploadResult result = fileSystemService.upload(newFile, NAMESPACE, caller.getUserId(), caller);

        record.setFileId(result.getFileId())
                .setSize(newFile.getSize())
                .setEditable(isEditable(record.getExtension(), newFile.getSize()) ? 1 : 0)
                .setUpdateTime(new Date());
        this.updateById(record);

        fileSystemService.delete(oldFileId, caller);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatch(String[] ids, FileCallerContext caller) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        List<BizServerFile> records = this.listByIds(Arrays.asList(ids));
        for (BizServerFile record : records) {
            fileSystemService.delete(record.getFileId(), caller);
        }
        return this.removeByIds(Arrays.asList(ids)) ? records.size() : 0;
    }

    private BizServerFile getValidById(String id) {
        BizServerFile record = this.getById(id);
        if (record == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        return record;
    }

    private boolean isEditable(String extension, long size) {
        return extension != null && EDITABLE_EXTENSIONS.contains(extension) && size <= EDITABLE_MAX_SIZE;
    }

}
