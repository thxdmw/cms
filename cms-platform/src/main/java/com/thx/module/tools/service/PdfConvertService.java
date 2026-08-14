package com.thx.module.tools.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * PDF 转 Word、图片 OCR 识别服务：封装按 IP 限流、文件大小校验、调用外部 Python 服务这些逻辑，
 * 供 {@link com.thx.module.tools.controller.PdfConvertController} 调用。
 */
public interface PdfConvertService {

    /**
     * 调用 Python 服务把 PDF 转换成 Word，返回 docx 文件的字节内容
     */
    byte[] pdfToWord(MultipartFile file, String clientIp);

    /**
     * 查询该 IP 当天 PDF 转 Word 剩余可用次数
     */
    Map<String, Object> getRemainingPdfCount(String clientIp);

    /**
     * OCR 图片识别文字（标准版，只返回文字/置信度/语言）
     */
    Map<String, Object> imageToText(MultipartFile file, String lang, String clientIp);

    /**
     * OCR 图片识别文字（详细版，额外返回逐词识别结果）
     */
    Map<String, Object> imageToTextDetailed(MultipartFile file, String lang, String clientIp);

    /**
     * 查询该 IP 当天 OCR 识别剩余可用次数
     */
    Map<String, Object> getRemainingOcrCount(String clientIp);

}
