package com.thx.module.file.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.module.file.annotation.RequiredFileScope;
import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.exception.FileSystemException;
import com.thx.module.file.interceptor.FileAuthInterceptor;
import com.thx.module.file.service.FileSystemService;
import com.thx.module.file.vo.FileInfoResult;
import com.thx.module.file.vo.FileUploadResult;
import com.thx.module.file.vo.FileUrlResult;
import com.thx.module.file.vo.ResponseVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * 文件管理 REST 接口
 * 所有请求需通过 FileAuthInterceptor 的 API Key 认证，appId 一律来自
 * FileCallerContext，禁止从请求参数接受 appId
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    /** 文件系统核心门面，appId 之外的具体业务逻辑全部委托给它 */
    private final FileSystemService fileSystemService;

    /**
     * 上传文件
     * @param file      待上传文件
     * @param namespace App 内的业务场景，决定使用哪个 FilePolicy
     * @param ownerId   文件所有者用户 ID，Policy 为 OWNER_ONLY 时必填
     */
    @RequiredFileScope("UPLOAD")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseVo<FileUploadResult> upload(@RequestParam("file") MultipartFile file,
                                                @RequestParam("namespace") String namespace,
                                                @RequestParam(value = "ownerId", required = false) String ownerId,
                                                HttpServletRequest request) {
        FileUploadResult result = fileSystemService.upload(file, namespace, ownerId, callerContext(request));
        return ResponseVo.success(result);
    }

    /** 查询文件元数据 */
    @RequiredFileScope("READ")
    @GetMapping("/{fileId}")
    public ResponseVo<FileInfoResult> getFileInfo(@PathVariable String fileId, HttpServletRequest request) {
        return ResponseVo.success(fileSystemService.get(fileId, callerContext(request)));
    }

    /** 分页查询当前 App 下的文件列表，namespace 可选 */
    @RequiredFileScope("LIST")
    @GetMapping
    public ResponseVo<IPage<FileInfoResult>> list(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String namespace,
                                                   HttpServletRequest request) {
        return ResponseVo.success(fileSystemService.list(page, size, namespace, callerContext(request)));
    }

    /** 获取文件下载 URL（PUBLIC 为公开地址，其余为一次性 Presigned URL） */
    @RequiredFileScope("PRESIGN")
    @PostMapping("/{fileId}/download-url")
    public ResponseVo<FileUrlResult> getDownloadUrl(@PathVariable String fileId, HttpServletRequest request) {
        String url = fileSystemService.getDownloadUrl(fileId, callerContext(request));
        return ResponseVo.success(new FileUrlResult(fileId, url));
    }

    /** 逻辑删除文件 */
    @RequiredFileScope("DELETE")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{fileId}")
    public void delete(@PathVariable String fileId, HttpServletRequest request) {
        fileSystemService.delete(fileId, callerContext(request));
    }

    /** 从请求属性中取出 FileAuthInterceptor 认证成功后写入的调用方上下文 */
    private FileCallerContext callerContext(HttpServletRequest request) {
        Object attr = request.getAttribute(FileAuthInterceptor.CALLER_CONTEXT_ATTR);
        if (!(attr instanceof FileCallerContext)) {
            throw new FileSystemException(401, "UNAUTHENTICATED", "未认证的请求");
        }
        return (FileCallerContext) attr;
    }
}
