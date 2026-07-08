package com.thx.common.util;

import cn.hutool.core.io.file.FileNameUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本地文件上传工具类，负责将上传的 {@link MultipartFile} 落盘到本地磁盘目录。
 *
 * @author tanghaixin
 * @date 2020/4/18 12:28 下午
 */
@Slf4j
@UtilityClass
public class FileUploadUtil {

    /** 匹配 Windows 路径分隔符 "\" 的模式，用于将本地路径统一替换为 URL 风格的 "/"。 */
    private final Pattern PATTERN = Pattern.compile("\\\\", Pattern.LITERAL);

    /**
     * 将上传文件保存到 uploadPath 下按当天日期（yyyyMMdd）分目录存放，文件名会被清洗
     * 并追加当前时间戳以避免重名覆盖。
     *
     * @param file       上传的文件
     * @param uploadPath 上传根目录
     * @return 相对路径，形如 "yyyyMMdd/文件名_时间戳.后缀"（统一使用 "/" 分隔）；
     *         目录创建失败或写盘异常时返回 null 或空字符串
     */
    public String uploadLocal(MultipartFile file, String uploadPath) {
        String res = "";
        try {
            String nowdayStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String uploadFullPath = uploadPath + File.separator + nowdayStr + File.separator;
            File fileDir = new File(uploadFullPath);
            // 创建文件根目录
            if (!fileDir.exists() && !fileDir.mkdirs()) {
                log.error("创建文件夹失败: {}", uploadFullPath);
                return null;
            }
            // 获取文件名
            String fileName = FileNameUtil.cleanInvalid(file.getOriginalFilename());
            fileName = FileNameUtil.getPrefix(fileName) + '_' + System.currentTimeMillis() + '.' + FileNameUtil.getSuffix(fileName);

            String savePath = fileDir.getPath() + File.separator + fileName;
            File savefile = new File(savePath);
            FileCopyUtils.copy(file.getBytes(), savefile);
            res = nowdayStr + File.separator + fileName;
            if (res.contains("\\")) {
                res = PATTERN.matcher(res).replaceAll(Matcher.quoteReplacement("/"));
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return res;
    }
}
