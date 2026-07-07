package com.thx.common.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.thx.common.config.properties.HttpLoggingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 将 HTTP 元数据和请求正文转换为可安全写入日志的内容。
 */
@Component
@RequiredArgsConstructor
public class HttpLogSanitizer {

    static final String MASK = "***";

    private final ObjectMapper objectMapper;
    private final HttpLoggingProperties properties;

    public Map<String, String> sanitizeHeaders(HttpServletRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return result;
        }
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (properties.getIncludedHeaders().contains(name.toLowerCase(Locale.ROOT))) {
                result.put(name.toLowerCase(Locale.ROOT), limit(request.getHeader(name), 512));
            }
        }
        return result;
    }

    public Map<String, List<String>> sanitizeQuery(String rawQuery, String encoding) {
        if (!StringUtils.hasText(rawQuery)) {
            return Collections.emptyMap();
        }

        Charset charset = resolveCharset(encoding);
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String name = decode(parts[0], charset);
            String value = parts.length == 2 ? decode(parts[1], charset) : "";
            result.computeIfAbsent(name, key -> new ArrayList<>())
                    .add(isSensitive(name) ? MASK : limit(value, 1024));
        }
        return result;
    }

    /**
     * 返回 JsonNode 或表单参数 Map，以保持访问日志结构化。
     * 对不支持、格式错误或被截断的正文直接省略，避免未脱敏的凭据泄露。
     */
    public Object sanitizePayload(byte[] content, String contentType, String encoding, boolean truncated) {
        if (content == null || content.length == 0 || !StringUtils.hasText(contentType)) {
            return null;
        }
        if (truncated) {
            return "<omitted: payload exceeds log limit>";
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException e) {
            return null;
        }

        String text = new String(content, resolveCharset(encoding));
        if (isJson(mediaType)) {
            try {
                JsonNode root = objectMapper.readTree(text);
                maskJson(root);
                return root;
            } catch (Exception e) {
                return "<omitted: malformed JSON>";
            }
        }
        if (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)) {
            return sanitizeQuery(text, encoding);
        }
        return null;
    }

    private void maskJson(JsonNode node) {
        if (node instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                if (isSensitive(fieldName)) {
                    objectNode.put(fieldName, MASK);
                } else {
                    maskJson(objectNode.get(fieldName));
                }
            }
        } else if (node instanceof ArrayNode) {
            for (JsonNode item : node) {
                maskJson(item);
            }
        }
    }

    private boolean isSensitive(String fieldName) {
        return properties.getSensitiveFields().contains(normalize(fieldName));
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private boolean isJson(MediaType mediaType) {
        String subtype = mediaType.getSubtype();
        return MediaType.APPLICATION_JSON.includes(mediaType)
                || "json".equalsIgnoreCase(subtype)
                || subtype.toLowerCase(Locale.ROOT).endsWith("+json");
    }

    private Charset resolveCharset(String encoding) {
        if (!StringUtils.hasText(encoding)) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private String decode(String value, Charset charset) {
        try {
            return URLDecoder.decode(value, charset.name());
        } catch (Exception e) {
            return value;
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...(truncated)";
    }
}
