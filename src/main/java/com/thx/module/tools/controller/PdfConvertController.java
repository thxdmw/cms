package com.thx.module.tools.controller;

import com.thx.common.util.IpUtil;
import com.thx.module.admin.vo.base.ResponseVo;
import com.thx.module.tools.service.PdfConvertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * PDF 转 Word、图片 OCR 识别接口。具体的限流/校验/外部服务调用逻辑都在
 * {@link PdfConvertService} 里，这里只负责接收请求参数、委托给 Service、写回响应。
 */
@Slf4j
@RestController
@RequestMapping("/tools/api/")
@RequiredArgsConstructor
public class PdfConvertController {

    private final PdfConvertService pdfConvertService;

    /**
     * PDF 转 Word，直接把转换结果作为 docx 附件流式返回
     */
    @PostMapping("/pdf-to-word")
    public void pdfToWord(@RequestPart("file") MultipartFile file, HttpServletRequest request, HttpServletResponse response) throws IOException {
        byte[] wordBytes = pdfConvertService.pdfToWord(file, IpUtil.getIpAddr(request));
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=converted.docx");
        response.getOutputStream().write(wordBytes);
    }

    /**
     * 查询当前 IP 今日 PDF 转 Word 剩余可用次数
     */
    @GetMapping("/pdf-to-word/remaining-count")
    public ResponseVo<Map<String, Object>> getRemainingCount(HttpServletRequest request) {
        return ResponseVo.success("获取剩余次数成功", pdfConvertService.getRemainingPdfCount(IpUtil.getIpAddr(request)));
    }

    /**
     * OCR 图片识别文字（标准版）
     * POST /tools/api/ocr/image-to-text
     */
    @PostMapping("/ocr/image-to-text")
    public ResponseVo<Map<String, Object>> imageToText(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "lang", required = false, defaultValue = "chi_sim+eng") String lang,
            HttpServletRequest request) {
        return ResponseVo.success("识别成功", pdfConvertService.imageToText(file, lang, IpUtil.getIpAddr(request)));
    }

    /**
     * OCR 图片识别文字（详细版，额外返回逐词识别结果）
     * POST /tools/api/ocr/image-to-text-detailed
     */
    @PostMapping("/ocr/image-to-text-detailed")
    public ResponseVo<Map<String, Object>> imageToTextDetailed(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "lang", required = false, defaultValue = "chi_sim+eng") String lang,
            HttpServletRequest request) {
        return ResponseVo.success("识别成功", pdfConvertService.imageToTextDetailed(file, lang, IpUtil.getIpAddr(request)));
    }

    /**
     * 查询当前 IP 今日 OCR 识别剩余可用次数
     * GET /tools/api/ocr/remaining-count
     */
    @GetMapping("/ocr/remaining-count")
    public ResponseVo<Map<String, Object>> getOcrRemainingCount(HttpServletRequest request) {
        return ResponseVo.success("获取剩余次数成功", pdfConvertService.getRemainingOcrCount(IpUtil.getIpAddr(request)));
    }

}