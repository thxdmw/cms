package com.thx.module.admin.controller;

import com.alibaba.fastjson.JSONObject;
import com.thx.common.util.CoreConst;
import com.thx.module.admin.entity.User;
import com.thx.module.admin.vo.UploadResponse;
import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.context.FileCallerContextFactory;
import com.thx.module.file.exception.FileSystemException;
import com.thx.module.file.service.FileSystemService;
import com.thx.module.file.vo.FileUploadResult;
import cn.hutool.core.io.file.FileNameUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * 后台文件上传接口
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2019年9月11日
 */
@Slf4j
@Controller
@RequestMapping("/attachment")
@AllArgsConstructor
public class UploadController {

    /** CMS 通用附件场景，对应 file_app_namespace(cms, attachment) -> PRIVATE_FILE 策略 */
    private static final String NAMESPACE_ATTACHMENT = "attachment";
    /** CMS 文章图片场景，对应 file_app_namespace(cms, article-image) -> PUBLIC_IMAGE 策略 */
    private static final String NAMESPACE_ARTICLE_IMAGE = "article-image";

    private final FileSystemService fileSystemService;

    /**
     * 通用附件上传接口（后台附件，按当前登录用户归属）
     */
    @ResponseBody
    @PostMapping("/upload")
    public UploadResponse upload(@RequestParam(value = "file") MultipartFile file, HttpServletRequest request) {
        try {
            String userId = currentUserId();
            FileCallerContext caller = FileCallerContextFactory.forCms(userId, request.getRemoteAddr());
            FileUploadResult result = fileSystemService.upload(file, NAMESPACE_ATTACHMENT, userId, caller);
            return UploadResponse.success(result.getFileId(), file.getOriginalFilename(),
                    FileNameUtil.getSuffix(file.getOriginalFilename()),
                    result.getUrl(), CoreConst.SUCCESS_CODE);
        } catch (FileSystemException e) {
            log.warn("Upload rejected: {}", e.getMessage());
            return UploadResponse.failed(CoreConst.FAIL_CODE, e.getMessage());
        } catch (Exception e) {
            log.error("Upload failed", e);
            return UploadResponse.failed(CoreConst.FAIL_CODE, "上传失败");
        }
    }

    /**
     * ditor_md 编辑器上传图片（公开图片，不归属特定用户）
     * {
     * success : 0 | 1,           // 0 表示上传失败，1 表示上传成功
     * message : "提示的信息，上传成功或上传失败及错误信息等。",
     * url     : "图片地址"        // 上传成功时才返回
     * }
     */
    @ResponseBody
    @RequestMapping(value = "/uploadForEditor", method = RequestMethod.POST)
    public JSONObject uploadEdFile(@RequestParam("editormd-image-file") MultipartFile file, HttpServletRequest request) {
        JSONObject jsonObject = new JSONObject();
        try {
            String userId = currentUserId();
            FileCallerContext caller = FileCallerContextFactory.forCms(userId, request.getRemoteAddr());
            FileUploadResult result = fileSystemService.upload(file, NAMESPACE_ARTICLE_IMAGE, null, caller);
            jsonObject.put("success", 1);
            jsonObject.put("message", "上传成功");
            jsonObject.put("url", result.getUrl());
            jsonObject.put("fileId", result.getFileId());
        } catch (FileSystemException e) {
            log.warn("Editor upload rejected: {}", e.getMessage());
            jsonObject.put("success", 0);
            jsonObject.put("message", "上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Editor upload failed", e);
            jsonObject.put("success", 0);
            jsonObject.put("message", "上传失败");
        }
        return jsonObject;
    }

    private String currentUserId() {
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        return user.getUserId();
    }

}
