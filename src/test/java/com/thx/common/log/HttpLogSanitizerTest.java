package com.thx.common.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thx.common.config.properties.HttpLoggingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpLogSanitizerTest {

    private HttpLogSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new HttpLogSanitizer(new ObjectMapper(), new HttpLoggingProperties());
    }

    @Test
    void masksSensitiveJsonFieldsAtEveryLevel() {
        String json = "{\"username\":\"alice\",\"password\":\"plain\","
                + "\"nested\":{\"access_token\":\"secret\"},"
                + "\"items\":[{\"apiKey\":\"key\"}]}";

        Object result = sanitizer.sanitizePayload(
                json.getBytes(StandardCharsets.UTF_8),
                "application/json",
                "UTF-8",
                false);

        assertThat(result).isInstanceOf(JsonNode.class);
        JsonNode node = (JsonNode) result;
        assertThat(node.path("username").asText()).isEqualTo("alice");
        assertThat(node.path("password").asText()).isEqualTo(HttpLogSanitizer.MASK);
        assertThat(node.path("nested").path("access_token").asText()).isEqualTo(HttpLogSanitizer.MASK);
        assertThat(node.path("items").get(0).path("apiKey").asText()).isEqualTo(HttpLogSanitizer.MASK);
    }

    @Test
    void masksQuerySecretsAndOnlyKeepsAllowListedHeaders() {
        Map<String, List<String>> query = sanitizer.sanitizeQuery(
                "keyword=spring+boot&token=top-secret", "UTF-8");

        assertThat(query.get("keyword")).containsExactly("spring boot");
        assertThat(query.get("token")).containsExactly(HttpLogSanitizer.MASK);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "JUnit");
        request.addHeader("Authorization", "Bearer should-never-be-logged");
        request.addHeader("Cookie", "SESSION=should-never-be-logged");

        Map<String, String> headers = sanitizer.sanitizeHeaders(request);
        assertThat(headers).containsEntry("user-agent", "JUnit");
        assertThat(headers).doesNotContainKeys("authorization", "cookie");
    }

    @Test
    void omitsTruncatedAndMalformedPayloadsWithoutLeakingContent() {
        Object truncated = sanitizer.sanitizePayload(
                "{\"password\":\"secret\"}".getBytes(StandardCharsets.UTF_8),
                "application/json",
                "UTF-8",
                true);
        Object malformed = sanitizer.sanitizePayload(
                "{\"password\":\"secret".getBytes(StandardCharsets.UTF_8),
                "application/json",
                "UTF-8",
                false);

        assertThat(truncated.toString()).doesNotContain("secret");
        assertThat(malformed.toString()).doesNotContain("secret");
    }
}
