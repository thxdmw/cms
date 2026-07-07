package com.thx.module.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.thx.common.util.IpUtil;
import com.thx.common.util.ResultUtil;
import com.thx.module.admin.entity.BizServerFile;
import com.thx.module.admin.entity.User;
import com.thx.module.admin.service.BizServerFileService;
import com.thx.module.admin.vo.base.PageResultVo;
import com.thx.module.admin.vo.base.ResponseVo;
import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.context.FileCallerContextFactory;
import lombok.AllArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 后台服务器文件管理：业务表（文件名/备注/是否可编辑等元数据）在 admin 模块自己维护，
 * 文件内容通过 FileSystemService 托管在 file 模块，file 模块本身不感知这个功能的存在
 *
 * @author tanghaixin
 * @version V1.0
 * @date 2026年7月6日
 */
@Controller
@RequestMapping("serverFile")
@AllArgsConstructor
public class ServerFileController {

    private final BizServerFileService serverFileService;

    @PostMapping("list")
    @ResponseBody
    public PageResultVo list(String keyword, Integer pageNumber, Integer pageSize) {
        IPage<BizServerFile> page = serverFileService.pageServerFiles(keyword, pageNumber, pageSize);
        return ResultUtil.table(page.getRecords(), page.getTotal());
    }

    @PostMapping("/upload")
    @ResponseBody
    public ResponseVo upload(@RequestParam("file") MultipartFile file, String remark, HttpServletRequest request) {
        try {
            User user = currentUser();
            serverFileService.uploadFile(file, remark, user.getNickname(), callerContext(request, user));
            return ResultUtil.success("上传成功");
        } catch (Exception e) {
            return ResultUtil.error("上传失败：" + e.getMessage());
        }
    }

    /** 通过同源接口写回附件流，确保浏览器按下载处理，而不是打开 MinIO 预签名链接预览 */
    @GetMapping("/download")
    public void download(String id, HttpServletRequest request, HttpServletResponse response) {
        HttpURLConnection connection = null;
        try {
            User user = currentUser();
            BizServerFile record = serverFileService.getById(id);
            String url = serverFileService.getDownloadUrl(id, callerContext(request, user));

            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(60_000);

            String fileName = record != null ? record.getOriginalName() : "download";
            String contentType = record != null && record.getContentType() != null
                    ? record.getContentType()
                    : "application/octet-stream";
            response.setContentType(contentType);
            if (record != null && record.getSize() != null) {
                response.setHeader("Content-Length", String.valueOf(record.getSize()));
            }
            response.setHeader("Content-Disposition", buildAttachmentHeader(fileName));

            try (InputStream in = connection.getInputStream();
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
        } catch (Exception e) {
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @GetMapping("/preview")
    @ResponseBody
    public ResponseVo<String> preview(String id, HttpServletRequest request) {
        try {
            User user = currentUser();
            String content = serverFileService.previewContent(id, callerContext(request, user));
            return ResultUtil.success("获取成功", content);
        } catch (Exception e) {
            return ResultUtil.error("预览失败：" + e.getMessage());
        }
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseVo save(String id, String content, HttpServletRequest request) {
        try {
            User user = currentUser();
            serverFileService.saveContent(id, content, callerContext(request, user));
            return ResultUtil.success("保存成功");
        } catch (Exception e) {
            return ResultUtil.error("保存失败：" + e.getMessage());
        }
    }

    @PostMapping("/delete")
    @ResponseBody
    public ResponseVo delete(String id, HttpServletRequest request) {
        return deleteBatch(new String[]{id}, request);
    }

    @PostMapping("/batch/delete")
    @ResponseBody
    public ResponseVo deleteBatch(@RequestParam("ids[]") String[] ids, HttpServletRequest request) {
        try {
            User user = currentUser();
            int i = serverFileService.deleteBatch(ids, callerContext(request, user));
            if (i > 0) {
                return ResultUtil.success("删除成功");
            } else {
                return ResultUtil.error("删除失败");
            }
        } catch (Exception e) {
            return ResultUtil.error("删除失败：" + e.getMessage());
        }
    }

    private User currentUser() {
        return (User) SecurityUtils.getSubject().getPrincipal();
    }

    private FileCallerContext callerContext(HttpServletRequest request, User user) {
        return FileCallerContextFactory.forCms(user.getUserId(), IpUtil.getIpAddr(request));
    }

    private String buildAttachmentHeader(String fileName) throws Exception {
        String fallback = fileName
                .replaceAll("[\\/:*?\"<>|\r\n]+", "_")
                .replaceAll("[^\\x20-\\x7E]+", "_");
        if (fallback.trim().isEmpty()) {
            fallback = "download";
        }
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        return "attachment; filename=\"" + fallback + "\"; filename*=UTF-8''" + encoded;
    }
}
