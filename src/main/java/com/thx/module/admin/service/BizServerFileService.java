package com.thx.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizServerFile;
import com.thx.module.file.context.FileCallerContext;
import org.springframework.web.multipart.MultipartFile;

/**
 * 服务器文件管理：admin 模块自己的业务表，文件内容托管在 file 模块（通过 FileSystemService 调用），
 * 保持 file 模块本身不感知这个功能的存在
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2026年7月6日
 */
public interface BizServerFileService extends IService<BizServerFile> {

    IPage<BizServerFile> pageServerFiles(String keyword, Integer pageNumber, Integer pageSize);

    BizServerFile uploadFile(MultipartFile file, String remark, String uploader, FileCallerContext caller);

    /** 获取文件下载 URL（来自 file 模块的一次性 Presigned URL 或公开地址） */
    String getDownloadUrl(String id, FileCallerContext caller);

    /** 读取文本文件内容用于在线预览，仅限 editable=1 的记录 */
    String previewContent(String id, FileCallerContext caller);

    /** 在线编辑保存：上传新内容得到新 fileId，更新记录后删除旧 fileId 对应的文件 */
    void saveContent(String id, String content, FileCallerContext caller);

    int deleteBatch(String[] ids, FileCallerContext caller);

}
