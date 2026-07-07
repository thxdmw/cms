package com.thx.common.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thx.common.config.properties.HttpLoggingProperties;
import com.thx.module.admin.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP 接口访问日志过滤器：统一记录链路 ID、安全请求元数据、耗时、路由、处理方法、
 * 调用方、请求与响应字节数，并可按配置记录脱敏后的正文。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class HttpAccessLogFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_ATTRIBUTE = HttpAccessLogFilter.class.getName() + ".traceId";
    public static final String HANDLER_ATTRIBUTE = HttpAccessLogFilter.class.getName() + ".handler";
    public static final String CALLER_ATTRIBUTE = HttpAccessLogFilter.class.getName() + ".caller";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("HTTP_ACCESS");
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");
    private static final Pattern TRACE_PARENT = Pattern.compile(
            "^[\\da-fA-F]{2}-([\\da-fA-F]{32})-[\\da-fA-F]{16}-[\\da-fA-F]{2}$");
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final HttpLoggingProperties properties;
    private final HttpLogSanitizer sanitizer;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        for (String pattern : properties.getExcludedPaths()) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        int payloadLimit = Math.max(256, properties.getMaxPayloadLength());
        String traceId = resolveTraceId(request);
        String previousTraceId = MDC.get("traceId");
        MDC.put("traceId", traceId);
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        response.setHeader(REQUEST_ID_HEADER, traceId);

        HttpServletRequest tracedRequest = new TraceIdRequestWrapper(request, traceId);
        HttpServletRequest requestToUse = properties.isIncludeRequestBody()
                ? new ContentCachingRequestWrapper(tracedRequest, payloadLimit)
                : tracedRequest;
        CapturingResponseWrapper responseToUse = new CapturingResponseWrapper(
                response,
                properties.isIncludeResponseBody() ? payloadLimit : 0);

        long startedAt = System.nanoTime();
        Exception failure = null;
        try {
            filterChain.doFilter(requestToUse, responseToUse);
        } catch (Exception e) {
            failure = e;
            log.error("Unhandled HTTP request exception: method={}, path={}",
                    request.getMethod(), request.getRequestURI(), e);
            throw e;
        } finally {
            responseToUse.flushCapturedWriter();
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
            writeAccessLog(requestToUse, responseToUse, traceId, durationMs, failure, payloadLimit);
            restoreMdc(previousTraceId);
        }
    }

    private void writeAccessLog(
            HttpServletRequest request,
            CapturingResponseWrapper response,
            String traceId,
            long durationMs,
            Exception failure,
            int payloadLimit) {

        int status = failure != null && response.getStatus() < 400
                ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                : response.getStatus();

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event", "http_access");
        event.put("traceId", traceId);
        event.put("method", request.getMethod());
        event.put("path", request.getRequestURI());

        Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (route != null) {
            event.put("route", route);
        }
        Object handler = request.getAttribute(HANDLER_ATTRIBUTE);
        if (handler != null) {
            event.put("handler", handler);
        }

        event.put("status", status);
        event.put("durationMs", durationMs);
        event.put("slow", durationMs >= properties.getSlowRequestThresholdMs());
        event.put("clientIp", resolveClientIp(request));
        event.put("scheme", request.getScheme());
        event.put("protocol", request.getProtocol());

        Object requestCaller = request.getAttribute(CALLER_ATTRIBUTE);
        String caller = requestCaller == null
                ? resolveCaller() : requestCaller.toString();
        if (caller != null) {
            event.put("caller", caller);
        }

        Map<String, String> headers = sanitizer.sanitizeHeaders(request);
        if (!headers.isEmpty()) {
            event.put("headers", headers);
        }
        if (StringUtils.hasText(request.getQueryString())) {
            event.put("query", sanitizer.sanitizeQuery(
                    request.getQueryString(), request.getCharacterEncoding()));
        }

        long requestBytes = request.getContentLengthLong();
        if (requestBytes >= 0) {
            event.put("requestBytes", requestBytes);
        }
        event.put("responseBytes", response.getWrittenByteCount());

        addRequestBody(event, request, payloadLimit);
        addResponseBody(event, response);

        if (failure != null) {
            event.put("exception", failure.getClass().getName());
            event.put("exceptionMessage", safeExceptionMessage(failure.getMessage()));
        }

        String message = toJson(event);
        if (status >= 500) {
            ACCESS_LOG.error(message);
        } else if (status >= 400 || durationMs >= properties.getSlowRequestThresholdMs()) {
            ACCESS_LOG.warn(message);
        } else {
            ACCESS_LOG.info(message);
        }
    }

    private void addRequestBody(Map<String, Object> event, HttpServletRequest request, int payloadLimit) {
        if (!properties.isIncludeRequestBody() || !(request instanceof ContentCachingRequestWrapper)) {
            return;
        }
        byte[] content = ((ContentCachingRequestWrapper) request).getContentAsByteArray();
        boolean truncated = content.length >= payloadLimit
                || (request.getContentLengthLong() > content.length && content.length > 0);
        Object safePayload = sanitizer.sanitizePayload(
                content, request.getContentType(), request.getCharacterEncoding(), truncated);
        if (safePayload != null) {
            event.put("requestBody", safePayload);
        }
    }

    private void addResponseBody(Map<String, Object> event, CapturingResponseWrapper response) {
        if (!properties.isIncludeResponseBody()) {
            return;
        }
        Object safePayload = sanitizer.sanitizePayload(
                response.getCapturedContent(),
                response.getContentType(),
                response.getCharacterEncoding(),
                response.isCaptureTruncated());
        if (safePayload != null) {
            event.put("responseBody", safePayload);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (StringUtils.hasText(requestId) && SAFE_REQUEST_ID.matcher(requestId.trim()).matches()) {
            return requestId.trim();
        }

        String traceParent = request.getHeader("traceparent");
        if (StringUtils.hasText(traceParent)) {
            Matcher matcher = TRACE_PARENT.matcher(traceParent.trim());
            if (matcher.matches() && !matcher.group(1).matches("0{32}")) {
                return matcher.group(1).toLowerCase(Locale.ROOT);
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp.trim() : request.getRemoteAddr();
    }

    private String resolveCaller() {
        try {
            Subject subject = SecurityUtils.getSubject();
            Object principal = subject == null ? null : subject.getPrincipal();
            if (principal instanceof User) {
                return ((User) principal).getUsername();
            }
            if (principal instanceof String) {
                return (String) principal;
            }
        } catch (Exception ignored) {
            // 匿名访问时 Shiro 可能尚未创建或绑定 Subject，此处不影响主请求。
        }
        return null;
    }

    private String toJson(Map<String, Object> event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return "{\"event\":\"http_access\",\"serializationError\":true}";
        }
    }

    private String safeExceptionMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        String singleLine = message.replace('\r', ' ').replace('\n', ' ');
        return singleLine.length() <= 512
                ? singleLine
                : singleLine.substring(0, 512) + "...(truncated)";
    }

    private void restoreMdc(String previousTraceId) {
        if (previousTraceId == null) {
            MDC.remove("traceId");
        } else {
            MDC.put("traceId", previousTraceId);
        }
    }

    /**
     * 将服务端生成的请求 ID 以请求头形式提供给下游拦截器和业务服务，
     * 使其与调用方主动传入请求 ID 时的读取方式保持一致。
     */
    private static final class TraceIdRequestWrapper extends HttpServletRequestWrapper {

        private final String traceId;

        private TraceIdRequestWrapper(HttpServletRequest request, String traceId) {
            super(request);
            this.traceId = traceId;
        }

        @Override
        public String getHeader(String name) {
            if (REQUEST_ID_HEADER.equalsIgnoreCase(name)) {
                return traceId;
            }
            return super.getHeader(name);
        }
    }

    /**
     * 响应内容直接写给客户端，只保留配置上限内的前若干字节用于排查问题。
     * 大文件下载仅统计总字节数，不会把完整内容缓存在内存中。
     */
    private static final class CapturingResponseWrapper extends HttpServletResponseWrapper {

        private final int captureLimit;
        private final ByteArrayOutputStream captured;
        private long writtenByteCount;
        private ServletOutputStream outputStream;
        private PrintWriter writer;
        private boolean outputStreamRequested;
        private boolean writerRequested;

        private CapturingResponseWrapper(HttpServletResponse response, int captureLimit) {
            super(response);
            this.captureLimit = Math.max(0, captureLimit);
            this.captured = new ByteArrayOutputStream(Math.min(this.captureLimit, 1024));
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (writerRequested) {
                throw new IllegalStateException("getWriter() has already been called for this response");
            }
            outputStreamRequested = true;
            if (outputStream == null) {
                outputStream = createOutputStream();
            }
            return outputStream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (outputStreamRequested) {
                throw new IllegalStateException("getOutputStream() has already been called for this response");
            }
            writerRequested = true;
            if (writer == null) {
                outputStream = createOutputStream();
                String encoding = getCharacterEncoding() == null
                        ? StandardCharsets.UTF_8.name()
                        : getCharacterEncoding();
                writer = new PrintWriter(new OutputStreamWriter(outputStream, encoding));
            }
            return writer;
        }

        @Override
        public void flushBuffer() throws IOException {
            flushCapturedWriter();
            if (outputStream != null) {
                outputStream.flush();
            }
            super.flushBuffer();
        }

        private ServletOutputStream createOutputStream() throws IOException {
            ServletOutputStream delegate = ((HttpServletResponse) getResponse()).getOutputStream();
            return new ServletOutputStream() {
                @Override
                public boolean isReady() {
                    return delegate.isReady();
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                    delegate.setWriteListener(writeListener);
                }

                @Override
                public void write(int value) throws IOException {
                    delegate.write(value);
                    capture(new byte[]{(byte) value}, 0, 1);
                }

                @Override
                public void write(byte[] bytes, int offset, int length) throws IOException {
                    delegate.write(bytes, offset, length);
                    capture(bytes, offset, length);
                }
            };
        }

        private void capture(byte[] bytes, int offset, int length) {
            writtenByteCount += length;
            int remaining = captureLimit - captured.size();
            if (remaining > 0) {
                captured.write(bytes, offset, Math.min(remaining, length));
            }
        }

        private void flushCapturedWriter() {
            if (writer != null) {
                writer.flush();
            }
        }

        private byte[] getCapturedContent() {
            return captured.toByteArray();
        }

        private long getWrittenByteCount() {
            return writtenByteCount;
        }

        private boolean isCaptureTruncated() {
            return writtenByteCount > captured.size();
        }
    }
}
