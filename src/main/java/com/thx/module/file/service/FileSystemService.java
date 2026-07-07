package com.thx.module.file.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.vo.FileInfoResult;
import com.thx.module.file.vo.FileUploadResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件系统核心门面
 * 只依赖 FileCallerContext，不直接读取 HttpServletRequest / Header / Shiro Subject
 */
public interface FileSystemService {

    /**
     * 上传文件
     * @param file    要上传的文件
     * @param namespace App 内的业务场景，决定使用哪个 FilePolicy
     * @param ownerId 文件所有者用户 ID；Policy 为 OWNER_ONLY 时必填
     * @param caller  调用方上下文
     */
    FileUploadResult upload(MultipartFile file, String namespace, String ownerId, FileCallerContext caller);

    /** 获取文件元数据 */
    FileInfoResult get(String fileId, FileCallerContext caller);

    /** 获取文件下载 URL（PUBLIC 为公开地址，其余为 Presigned URL） */
    String getDownloadUrl(String fileId, FileCallerContext caller);

    /** 逻辑删除文件 */
    void delete(String fileId, FileCallerContext caller);

    /** 分页查询当前 App 下的文件列表 */
    IPage<FileInfoResult> list(int page, int size, String namespace, FileCallerContext caller);
}
