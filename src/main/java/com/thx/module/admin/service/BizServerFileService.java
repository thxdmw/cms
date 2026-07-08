package com.thx.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.thx.module.admin.entity.BizServerFile;
import com.thx.module.file.context.FileCallerContext;
import org.springframework.web.multipart.MultipartFile;

/**
 * 服务器文件管理：admin 模块自己的业务表，文件内容托管在 file 模块（通过 FileSystemService 调用），
 * 保持 file 模块本身不感知这个功能的存在。
 * 是否支持在线预览/编辑由扩展名白名单和文件大小上限共同决定（见实现类中的 EDITABLE_EXTENSIONS / EDITABLE_MAX_SIZE）。
 */
public interface BizServerFileService extends IService<BizServerFile> {

    /**
     * 分页查询服务器文件记录，支持按原始文件名模糊匹配，按创建时间倒序排列。
     *
     * @param keyword    文件名关键字，模糊匹配，传空则不过滤
     * @param pageNumber 页码
     * @param pageSize   每页条数
     * @return 分页结果
     */
    IPage<BizServerFile> pageServerFiles(String keyword, Integer pageNumber, Integer pageSize);

    /**
     * 上传文件：委托 file 模块完成实际存储，再落地一条 biz_server_file 业务记录（含是否可在线预览/编辑的判定）。
     * 具有事务保护，落库失败会回滚。
     *
     * @param file     上传的文件
     * @param remark   备注
     * @param uploader 上传人展示名
     * @param caller   file 模块所需的调用方上下文（appId/userId 等）
     * @return 新建的文件记录
     */
    BizServerFile uploadFile(MultipartFile file, String remark, String uploader, FileCallerContext caller);

    /** 获取文件下载 URL（来自 file 模块的一次性 Presigned URL 或公开地址） */
    String getDownloadUrl(String id, FileCallerContext caller);

    /** 读取文本文件内容用于在线预览，仅限 editable=1 的记录，否则抛出 {@link IllegalArgumentException} */
    String previewContent(String id, FileCallerContext caller);

    /** 在线编辑保存：上传新内容得到新 fileId，更新记录后删除旧 fileId 对应的文件；仅限 editable=1 的记录，具有事务保护 */
    void saveContent(String id, String content, FileCallerContext caller);

    /**
     * 批量删除文件：先逐条删除 file 模块中的实际文件，再删除本地业务记录，具有事务保护。
     *
     * @param ids    文件记录 id 数组
     * @param caller file 模块所需的调用方上下文
     * @return 实际删除的记录数；ids 为空时返回 0
     */
    int deleteBatch(String[] ids, FileCallerContext caller);

}
