package com.thx.common.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thx.common.config.properties.HttpLoggingProperties;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class HttpAccessLogFilterTest {

    @Test
    void propagatesTraceIdAndWritesMaskedStructuredAccessLog() throws Exception {
        HttpLoggingProperties properties = new HttpLoggingProperties();
        properties.setIncludeRequestBody(true);
        properties.setIncludeResponseBody(true);
        properties.setExcludedPaths(Collections.emptyList());
        properties.setSlowRequestThresholdMs(60_000L);

        ObjectMapper objectMapper = new ObjectMapper();
        HttpLogSanitizer sanitizer = new HttpLogSanitizer(objectMapper, properties);
        HttpAccessLogFilter filter = new HttpAccessLogFilter(properties, sanitizer, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContentType("application/json");
        request.setContent("{\"product\":\"book\",\"password\":\"plain\"}"
                .getBytes(StandardCharsets.UTF_8));
        request.setQueryString("page=1&token=query-secret");
        request.addHeader(HttpAccessLogFilter.REQUEST_ID_HEADER, "client-request-123");
        request.addHeader("Authorization", "Bearer header-secret");

        MockHttpServletResponse response = new MockHttpServletResponse();
        Logger logger = (Logger) LoggerFactory.getLogger("HTTP_ACCESS");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            filter.doFilter(request, response, (wrappedRequest, wrappedResponse) -> {
                String body = StreamUtils.copyToString(
                        wrappedRequest.getInputStream(), StandardCharsets.UTF_8);
                assertThat(body).contains("\"product\":\"book\"");
                assertThat(wrappedRequest.getAttribute(HttpAccessLogFilter.TRACE_ID_ATTRIBUTE))
                        .isEqualTo("client-request-123");

                wrappedResponse.setContentType("application/json");
                wrappedResponse.getWriter().write(
                        "{\"result\":\"ok\",\"accessToken\":\"response-secret\"}");
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertThat(response.getHeader(HttpAccessLogFilter.REQUEST_ID_HEADER))
                .isEqualTo("client-request-123");
        assertThat(response.getContentAsString())
                .contains("\"result\":\"ok\"", "\"accessToken\":\"response-secret\"");
        assertThat(MDC.get("traceId")).isNull();
        assertThat(appender.list).hasSize(1);

        JsonNode event = objectMapper.readTree(appender.list.get(0).getFormattedMessage());
        assertThat(event.path("event").asText()).isEqualTo("http_access");
        assertThat(event.path("traceId").asText()).isEqualTo("client-request-123");
        assertThat(event.path("status").asInt()).isEqualTo(200);
        assertThat(event.path("query").path("token").get(0).asText())
                .isEqualTo(HttpLogSanitizer.MASK);
        assertThat(event.path("requestBody").path("password").asText())
                .isEqualTo(HttpLogSanitizer.MASK);
        assertThat(event.path("responseBody").path("accessToken").asText())
                .isEqualTo(HttpLogSanitizer.MASK);
        assertThat(event.toString()).doesNotContain("header-secret", "query-secret", "plain", "response-secret");
    }

    @Test
    void replacesUnsafeRequestId() throws Exception {
        HttpLoggingProperties properties = new HttpLoggingProperties();
        properties.setExcludedPaths(Collections.emptyList());
        ObjectMapper objectMapper = new ObjectMapper();
        HttpAccessLogFilter filter = new HttpAccessLogFilter(
                properties, new HttpLogSanitizer(objectMapper, properties), objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        request.addHeader(HttpAccessLogFilter.REQUEST_ID_HEADER, "bad id\r\ninjected");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (wrappedRequest, wrappedResponse) -> {
        });

        assertThat(response.getHeader(HttpAccessLogFilter.REQUEST_ID_HEADER))
                .matches("[a-f0-9]{32}")
                .doesNotContain("bad id");
    }
}
