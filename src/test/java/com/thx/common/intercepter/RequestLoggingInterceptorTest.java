package com.thx.common.intercepter;

import com.thx.common.log.HttpAccessLogFilter;
import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.interceptor.FileAuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingInterceptorTest {

    private final RequestLoggingInterceptor interceptor = new RequestLoggingInterceptor();

    @Test
    void addsHandlerAndApplicationCallerMetadata() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = new HandlerMethod(
                new SampleController(),
                SampleController.class.getDeclaredMethod("handle"));

        FileCallerContext caller = new FileCallerContext();
        caller.setAppId("document-service");
        request.setAttribute(FileAuthInterceptor.CALLER_CONTEXT_ATTR, caller);

        assertThat(interceptor.preHandle(request, response, handler)).isTrue();
        interceptor.afterCompletion(request, response, handler, null);

        assertThat(request.getAttribute(HttpAccessLogFilter.HANDLER_ATTRIBUTE))
                .isEqualTo("SampleController#handle");
        assertThat(request.getAttribute(HttpAccessLogFilter.CALLER_ATTRIBUTE))
                .isEqualTo("app:document-service");
    }

    private static final class SampleController {
        public void handle() {
        }
    }
}
