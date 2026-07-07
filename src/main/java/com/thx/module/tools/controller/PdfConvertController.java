package com.thx.module.tools.controller;

import com.thx.exception.ApiException;
import com.thx.module.admin.vo.base.ResponseVo;
import com.thx.module.tools.utils.FileSizeUtils;
import com.thx.module.tools.utils.MultipartInputStreamFileResource;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/tools/api/")
public class PdfConvertController implements Serializable {

    @Value("${python.server.url:http://localhost:8000}")
    private String pythonServerUrl;
    @Value("${pdf.max-size:10MB}") // 默认 10MB，支持带单位配置
    private String maxPdfSizeConfig;
    private Long maxPdfSizeBytes; // 缓存解析后的字节数
    
    @Value("${pdf.limit.count-per-ip:2}") // 每个 IP 每天限制次数
    private int countPerIp;
    
    @Value("${pdf.limit.reset-time:86400}") // 重置时间 (秒)，默认 24 小时
    private int resetTime;
        
    @Value("${ocr.max-size:10MB}") // 默认 10MB，支持带单位配置
    private String maxOcrSizeConfig;
    private Long maxOcrSizeBytes; // 缓存解析后的字节数
    
    @Value("${ocr.limit.count-per-ip:10}") // 每个 IP 每天限制次数
    private int ocrCountPerIp;
    
    @Value("${ocr.limit.reset-time:86400}") // 重置时间 (秒)，默认 24 小时
    private int ocrResetTime;
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

    @PostMapping("/pdf-to-word")
    public void pdfToWord(@RequestPart("file") MultipartFile file, HttpServletResponse response) {
        // 获取客户端IP
        String clientIp = getClientIp();
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

        // 1. 调用 Python 服务
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        long startTime = System.currentTimeMillis();
        try {
            body.add("file", new MultipartInputStreamFileResource(file));
            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, headers);

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

            // 2. 返回 Word 给前端
            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );
            response.setHeader(
                    "Content-Disposition",
                    "attachment; filename=converted.docx"
            );

            if (resp.getBody() != null) {
                // 增加访问次数并设置当天24点过期
                redisTemplate.opsForValue().increment(redisKey, 1);
                if (currentCount == 0) {
                    redisTemplate.expireAt(redisKey, getTodayEnd());
                }
                response.getOutputStream().write(Objects.requireNonNull(resp.getBody()));
                log.info("PDF转Word处理完成，返回给客户端的文件大小: {} 字节", resp.getBody().length);
            } else {
                log.warn("Python服务返回空响应体");
            }
        } catch (IOException e) {
            log.error("pdf转word文件异常，文件名: {}, 异常信息：{}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("PDF转换失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("调用Python服务异常，文件名: {}, 异常信息：{}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("服务调用失败: " + e.getMessage());
        }
    }

    @GetMapping("/pdf-to-word/remaining-count")
    public ResponseVo<Map<String, Object>> getRemainingCount() {
        String clientIp = getClientIp();
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
        return ResponseVo.success("获取剩余次数成功", result);
    }

    /**
     * OCR 图片识别文字（标准版）
     * POST /tools/api/ocr/image-to-text
     */
    @PostMapping("/ocr/image-to-text")
    public ResponseVo<Map<String, Object>> imageToText(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "lang", required = false, defaultValue = "chi_sim+eng") String lang) {
        
        String clientIp = getClientIp();
        String redisKey = "tools:ocr_limit:" + clientIp;
        
        // 检查剩余次数
        Integer currentCount = redisTemplate.opsForValue().get(redisKey);
        if (currentCount == null) {
            currentCount = 0;
        }
        if (currentCount >= ocrCountPerIp) {
            String errorMsg = String.format("今日 OCR 识别次数已达上限 (%d 次)，请明日再试", ocrCountPerIp);
            log.warn("IP {} OCR 识别次数超限：{}", clientIp, currentCount);
            throw new ApiException(errorMsg);
        }

        log.info("开始处理 OCR 图片转文字请求，文件名：{}, 文件大小：{} bytes, 文件类型：{}, 语言：{}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType(), lang);

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
        
        try {
            body.add("file", new MultipartInputStreamFileResource(file));
            body.add("lang", lang);
            
            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, headers);

            log.info("正在调用 Python OCR 服务：{}, 请求参数：lang={}", 
                    pythonServerUrl + "/api/ocr/image-to-text", lang);
            
            ResponseEntity<String> resp = restTemplate.exchange(
                    pythonServerUrl + "/api/ocr/image-to-text",
                    HttpMethod.POST,
                    request,
                    String.class
            );
            long endTime = System.currentTimeMillis();

            log.info("Python OCR 服务调用完成，响应状态码：{}, 响应内容：{}, 耗时：{} ms",
                    resp.getStatusCodeValue(),
                    resp.getBody(),
                    endTime - startTime);

            if (resp.getStatusCodeValue() == 200 && resp.getBody() != null) {
                JSONObject jsonResponse = JSON.parseObject(resp.getBody());
                
                if (jsonResponse.getBoolean("success")) {
                    // 增加访问次数并设置当天 24 点过期
                    redisTemplate.opsForValue().increment(redisKey, 1);
                    if (currentCount == 0) {
                        redisTemplate.expireAt(redisKey, getTodayEnd());
                    }
                    
                    Map<String, Object> result = new HashMap<>(4);
                    result.put("text", jsonResponse.getString("text"));
                    result.put("confidence", jsonResponse.getDouble("confidence"));
                    result.put("language", jsonResponse.getString("language"));
                    result.put("success", true);
                    
                    log.info("OCR 识别成功，文字长度：{}, 置信度：{}", 
                            jsonResponse.getString("text").length(), 
                            jsonResponse.getDouble("confidence"));
                    
                    return ResponseVo.success("识别成功", result);
                } else {
                    String errorMsg = jsonResponse.getString("error");
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "OCR 识别失败";
                    }
                    log.warn("OCR 识别失败：{}", errorMsg);
                    throw new ApiException(errorMsg);
                }
            } else {
                log.error("OCR 服务返回异常状态码：{}", resp.getStatusCodeValue());
                throw new ApiException("OCR 服务调用失败");
            }
            
        } catch (IOException e) {
            log.error("OCR 文件处理异常，文件名：{}, 异常信息：{}", file.getOriginalFilename(), e.getMessage(), e);
            throw new ApiException("文件处理失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("调用 Python OCR 服务异常，文件名：{}, 异常信息：{}", file.getOriginalFilename(), e.getMessage(), e);
            throw new ApiException("服务调用失败：" + e.getMessage());
        }
    }

    /**
     * OCR 图片识别文字（详细版）
     * POST /tools/api/ocr/image-to-text-detailed
     */
    @PostMapping("/ocr/image-to-text-detailed")
    public ResponseVo<Map<String, Object>> imageToTextDetailed(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "lang", required = false, defaultValue = "chi_sim+eng") String lang) {
        
        String clientIp = getClientIp();
        String redisKey = "tools:ocr_detailed_limit:" + clientIp;
        
        // 检查剩余次数
        Integer currentCount = redisTemplate.opsForValue().get(redisKey);
        if (currentCount == null) {
            currentCount = 0;
        }
        if (currentCount >= ocrCountPerIp) {
            String errorMsg = String.format("今日详细 OCR 识别次数已达上限 (%d 次)，请明日再试", ocrCountPerIp);
            log.warn("IP {} 详细 OCR 识别次数超限：{}", clientIp, currentCount);
            throw new ApiException(errorMsg);
        }

        log.info("开始处理详细版 OCR 图片转文字请求，文件名：{}, 文件大小：{} bytes, 文件类型：{}, 语言：{}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType(), lang);

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
        
        try {
            body.add("file", new MultipartInputStreamFileResource(file));
            body.add("lang", lang);
            
            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, headers);

            log.info("正在调用 Python 详细版 OCR 服务：{}, 请求参数：lang={}", 
                    pythonServerUrl + "/api/ocr/image-to-text-detailed", lang);
            
            ResponseEntity<String> resp = restTemplate.exchange(
                    pythonServerUrl + "/api/ocr/image-to-text-detailed",
                    HttpMethod.POST,
                    request,
                    String.class
            );
            long endTime = System.currentTimeMillis();

            log.info("Python 详细版 OCR 服务调用完成，响应状态码：{}, 响应内容：{}, 耗时：{} ms",
                    resp.getStatusCodeValue(),
                    resp.getBody(),
                    endTime - startTime);

            if (resp.getStatusCodeValue() == 200 && resp.getBody() != null) {
                JSONObject jsonResponse = JSON.parseObject(resp.getBody());
                
                if (jsonResponse.getBoolean("success")) {
                    // 增加访问次数并设置当天 24 点过期
                    redisTemplate.opsForValue().increment(redisKey, 1);
                    if (currentCount == 0) {
                        redisTemplate.expireAt(redisKey, getTodayEnd());
                    }
                    
                    Map<String, Object> result = new HashMap<>(4);
                    result.put("text", jsonResponse.getString("text"));
                    result.put("words", jsonResponse.getJSONArray("words").toJavaList(Map.class));
                    result.put("language", jsonResponse.getString("language"));
                    result.put("success", true);
                    
                    JSONArray words = jsonResponse.getJSONArray("words");
                    log.info("详细 OCR 识别成功，文字长度：{}, 单词数量：{}", 
                            jsonResponse.getString("text").length(), 
                            words.size());
                    
                    return ResponseVo.success("识别成功", result);
                } else {
                    String errorMsg = jsonResponse.getString("error");
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "详细 OCR 识别失败";
                    }
                    log.warn("详细 OCR 识别失败：{}", errorMsg);
                    throw new ApiException(errorMsg);
                }
            } else {
                log.error("详细 OCR 服务返回异常状态码：{}", resp.getStatusCodeValue());
                throw new ApiException("详细 OCR 服务调用失败");
            }
            
        } catch (IOException e) {
            log.error("详细 OCR 文件处理异常，文件名：{}, 异常信息：{}", file.getOriginalFilename(), e.getMessage(), e);
            throw new ApiException("文件处理失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("调用 Python 详细 OCR 服务异常，文件名：{}, 异常信息：{}", file.getOriginalFilename(), e.getMessage(), e);
            throw new ApiException("服务调用失败：" + e.getMessage());
        }
    }

    /**
     * 查询 OCR 剩余次数
     * GET /tools/api/ocr/remaining-count
     */
    @GetMapping("/ocr/remaining-count")
    public ResponseVo<Map<String, Object>> getOcrRemainingCount() {
        String clientIp = getClientIp();
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
        return ResponseVo.success("获取剩余次数成功", result);
    }


    // 添加获取客户端IP的方法
    private String getClientIp() {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }

        return ip;
    }

    // 添加获取当天结束时间的方法
    private java.util.Date getTodayEnd() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }
}