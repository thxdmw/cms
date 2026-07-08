package com.thx.module.tools.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.thx.exception.ApiException;
import com.thx.module.tools.service.PdfConvertService;
import com.thx.module.tools.util.FileSizeUtils;
import com.thx.module.tools.util.MultipartInputStreamFileResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link PdfConvertService} 实现：从 Controller 抽取出来的业务逻辑，
 * 包含配置解析、Redis 按 IP 限流、调用外部 Python 转换/OCR 服务这几块。
 */
@Slf4j
@Service
public class PdfConvertServiceImpl implements PdfConvertService {

    @Value("${python.server.url:http://localhost:8000}")
    private String pythonServerUrl;
    @Value("${pdf.max-size:10MB}") // 默认 10MB，支持带单位配置
    private String maxPdfSizeConfig;
    private Long maxPdfSizeBytes; // 缓存解析后的字节数

    @Value("${pdf.limit.count-per-ip:2}") // 每个 IP 每天限制次数
    private int countPerIp;

    @Value("${ocr.max-size:10MB}") // 默认 10MB，支持带单位配置
    private String maxOcrSizeConfig;
    private Long maxOcrSizeBytes; // 缓存解析后的字节数

    @Value("${ocr.limit.count-per-ip:10}") // 每个 IP 每天限制次数
    private int ocrCountPerIp;

    @Resource
    private RedisTemplate<String, Integer> redisTemplate;

    // 初始化时解析配置
    @PostConstruct
    public void init() {
        try {
            maxPdfSizeBytes = FileSizeUtils.parseFileSize(maxPdfSizeConfig);
            log.info("PDF 文件大小限制已设置为：{} ({})", maxPdfSizeConfig, maxPdfSizeBytes);
        } catch (Exception e) {
            log.error("解析 PDF 文件大小配置失败：{}", e.getMessage(), e);
            // 使用默认值 10MB
            maxPdfSizeBytes = 10L * 1024 * 1024;
            log.info("使用默认 PDF 文件大小限制：10MB ({})", maxPdfSizeBytes);
        }

        try {
            maxOcrSizeBytes = FileSizeUtils.parseFileSize(maxOcrSizeConfig);
            log.info("OCR 图片文件大小限制已设置为：{} ({})", maxOcrSizeConfig, maxOcrSizeBytes);
        } catch (Exception e) {
            log.error("解析 OCR 图片文件大小配置失败：{}", e.getMessage(), e);
            // 使用默认值 10MB
            maxOcrSizeBytes = 10L * 1024 * 1024;
            log.info("使用默认 OCR 图片文件大小限制：10MB ({})", maxOcrSizeBytes);
        }
    }

    @Override
    public byte[] pdfToWord(MultipartFile file, String clientIp) {
        String redisKey = "tools:pdf_convert_limit:" + clientIp;
        // 检查剩余次数
        Integer currentCount = redisTemplate.opsForValue().get(redisKey);
        if (currentCount == null) {
            currentCount = 0;
        }
        if (currentCount >= countPerIp) {
            String errorMsg = String.format("今日转换次数已达上限(%d次)，请明日再试", countPerIp);
            log.warn("IP {} 转换次数超限: {}", clientIp, currentCount);
            throw new ApiException(errorMsg);
        }

        log.info("开始处理PDF转Word请求，文件名: {}, 文件大小: {} bytes, 文件类型: {}", file.getOriginalFilename(), file.getSize(), file.getContentType());

        // 检查文件大小
        if (file.getSize() > maxPdfSizeBytes) {
            String errorMsg = String.format("文件大小超过限制，最大允许: %s (%d bytes)，当前文件: %d bytes", maxPdfSizeConfig, maxPdfSizeBytes, file.getSize());
            log.warn(errorMsg);
            throw new ApiException("文件大小超出限制: " + errorMsg);
        }

        // 调用 Python 服务
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        long startTime = System.currentTimeMillis();
        try {
            body.add("file", new MultipartInputStreamFileResource(file));
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("正在调用Python服务: {}, 请求头: {}", pythonServerUrl + "/api/convert/pdf-to-word", headers);
            ResponseEntity<byte[]> resp = restTemplate.exchange(
                    pythonServerUrl + "/api/convert/pdf-to-word",
                    HttpMethod.POST,
                    request,
                    byte[].class
            );
            long endTime = System.currentTimeMillis();

            log.info("Python服务调用完成，响应状态码: {}, 响应长度: {} bytes, 耗时: {} ms",
                    resp.getStatusCodeValue(),
                    resp.getBody() != null ? resp.getBody().length : 0,
                    endTime - startTime);

            if (resp.getBody() == null) {
                log.warn("Python服务返回空响应体");
                throw new ApiException("PDF 转换失败：转换服务未返回内容");
            }

            // 增加访问次数并设置当天24点过期
            redisTemplate.opsForValue().increment(redisKey, 1);
            if (currentCount == 0) {
                redisTemplate.expireAt(redisKey, getTodayEnd());
            }
            log.info("PDF转Word处理完成，文件大小: {} 字节", resp.getBody().length);
            return resp.getBody();
        } catch (IOException e) {
            log.error("pdf转word文件异常，文件名: {}, 异常信息：{}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("PDF转换失败: " + e.getMessage());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用Python服务异常，文件名: {}, 异常信息：{}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("服务调用失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getRemainingPdfCount(String clientIp) {
        String redisKey = "tools:pdf_convert_limit:" + clientIp;
        Integer currentCount = redisTemplate.opsForValue().get(redisKey);
        if (currentCount == null) {
            currentCount = 0;
        }
        int remainingCount = Math.max(0, countPerIp - currentCount);
        Map<String, Object> result = new HashMap<>(3);
        result.put("totalCount", countPerIp);
        result.put("usedCount", currentCount);
        result.put("remainingCount", remainingCount);
        return result;
    }

    @Override
    public Map<String, Object> imageToText(MultipartFile file, String lang, String clientIp) {
        return doOcr(file, lang, clientIp, "tools:ocr_limit:", false);
    }

    @Override
    public Map<String, Object> imageToTextDetailed(MultipartFile file, String lang, String clientIp) {
        return doOcr(file, lang, clientIp, "tools:ocr_detailed_limit:", true);
    }

    @Override
    public Map<String, Object> getRemainingOcrCount(String clientIp) {
        String redisKey = "tools:ocr_limit:" + clientIp;
        Integer currentCount = redisTemplate.opsForValue().get(redisKey);
        if (currentCount == null) {
            currentCount = 0;
        }
        int remainingCount = Math.max(0, ocrCountPerIp - currentCount);
        Map<String, Object> result = new HashMap<>(3);
        result.put("totalCount", ocrCountPerIp);
        result.put("usedCount", currentCount);
        result.put("remainingCount", remainingCount);
        return result;
    }

    /**
     * 标准版和详细版 OCR 请求参数、限流方式完全一致，只有"是否需要返回逐词识别结果"这一点不同，
     * 抽成一个私有方法用 detailed 参数区分，避免两份高度重复的代码。
     */
    private Map<String, Object> doOcr(MultipartFile file, String lang, String clientIp, String redisKeyPrefix, boolean detailed) {
        String redisKey = redisKeyPrefix + clientIp;

        // 检查剩余次数
        Integer currentCount = redisTemplate.opsForValue().get(redisKey);
        if (currentCount == null) {
            currentCount = 0;
        }
        if (currentCount >= ocrCountPerIp) {
            String errorMsg = String.format("今日%sOCR 识别次数已达上限 (%d 次)，请明日再试", detailed ? "详细 " : "", ocrCountPerIp);
            log.warn("IP {} {}OCR 识别次数超限：{}", clientIp, detailed ? "详细 " : "", currentCount);
            throw new ApiException(errorMsg);
        }

        log.info("开始处理{}OCR 图片转文字请求，文件名：{}, 文件大小：{} bytes, 文件类型：{}, 语言：{}",
                detailed ? "详细版 " : "", file.getOriginalFilename(), file.getSize(), file.getContentType(), lang);

        // 检查文件大小
        if (file.getSize() > maxOcrSizeBytes) {
            String errorMsg = String.format("文件超过限制，最大允许：%s (%d bytes)，当前文件：%d bytes",
                    maxOcrSizeConfig, maxOcrSizeBytes, file.getSize());
            log.warn(errorMsg);
            throw new ApiException("文件大小超出限制：" + errorMsg);
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        long startTime = System.currentTimeMillis();
        String pythonPath = detailed ? "/api/ocr/image-to-text-detailed" : "/api/ocr/image-to-text";

        try {
            body.add("file", new MultipartInputStreamFileResource(file));
            body.add("lang", lang);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("正在调用 Python{} OCR 服务：{}, 请求参数：lang={}", detailed ? " 详细版" : "", pythonServerUrl + pythonPath, lang);

            ResponseEntity<String> resp = restTemplate.exchange(
                    pythonServerUrl + pythonPath,
                    HttpMethod.POST,
                    request,
                    String.class
            );
            long endTime = System.currentTimeMillis();

            log.info("Python{} OCR 服务调用完成，响应状态码：{}, 响应内容：{}, 耗时：{} ms",
                    detailed ? " 详细版" : "", resp.getStatusCodeValue(), resp.getBody(), endTime - startTime);

            if (resp.getStatusCodeValue() != 200 || resp.getBody() == null) {
                log.error("{}OCR 服务返回异常状态码：{}", detailed ? "详细 " : "", resp.getStatusCodeValue());
                throw new ApiException(detailed ? "详细 OCR 服务调用失败" : "OCR 服务调用失败");
            }

            JSONObject jsonResponse = JSON.parseObject(resp.getBody());
            if (!jsonResponse.getBoolean("success")) {
                String errorMsg = jsonResponse.getString("error");
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = detailed ? "详细 OCR 识别失败" : "OCR 识别失败";
                }
                log.warn("{}OCR 识别失败：{}", detailed ? "详细 " : "", errorMsg);
                throw new ApiException(errorMsg);
            }

            // 增加访问次数并设置当天 24 点过期
            redisTemplate.opsForValue().increment(redisKey, 1);
            if (currentCount == 0) {
                redisTemplate.expireAt(redisKey, getTodayEnd());
            }

            Map<String, Object> result = new HashMap<>(4);
            result.put("text", jsonResponse.getString("text"));
            result.put("language", jsonResponse.getString("language"));
            result.put("success", true);
            if (detailed) {
                JSONArray words = jsonResponse.getJSONArray("words");
                result.put("words", words.toJavaList(Map.class));
                log.info("详细 OCR 识别成功，文字长度：{}, 单词数量：{}", jsonResponse.getString("text").length(), words.size());
            } else {
                result.put("confidence", jsonResponse.getDouble("confidence"));
                log.info("OCR 识别成功，文字长度：{}, 置信度：{}", jsonResponse.getString("text").length(), jsonResponse.getDouble("confidence"));
            }
            return result;
        } catch (IOException e) {
            log.error("{}OCR 文件处理异常，文件名：{}, 异常信息：{}", detailed ? "详细 " : "", file.getOriginalFilename(), e.getMessage(), e);
            throw new ApiException("文件处理失败：" + e.getMessage());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 Python{} OCR 服务异常，文件名：{}, 异常信息：{}", detailed ? " 详细" : "", file.getOriginalFilename(), e.getMessage(), e);
            throw new ApiException("服务调用失败：" + e.getMessage());
        }
    }

    /**
     * 获取当天结束时间（23:59:59.999），用于给限流计数器设置过期时间，实现"每天重置"的效果
     */
    private Date getTodayEnd() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }
}
