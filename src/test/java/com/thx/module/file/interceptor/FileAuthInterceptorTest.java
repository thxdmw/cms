package com.thx.module.file.interceptor;

import com.thx.module.file.annotation.RequiredFileScope;
import com.thx.module.file.config.FileSystemProperties;
import com.thx.module.file.context.FileCallerContext;
import com.thx.module.file.mapper.FileAppMapper;
import com.thx.module.file.model.FileApp;
import com.thx.module.file.util.ApiKeyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileAuthInterceptorTest {

    @Mock
    private FileAppMapper fileAppMapper;

    private FileSystemProperties properties;
    private FileAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        properties = new FileSystemProperties();
        properties.setEnabled(true);
        interceptor = new FileAuthInterceptor(fileAppMapper, properties);
    }

    /** 仅用于反射构造 HandlerMethod 的测试专用假控制器 */
    static class DummyController {
        @RequiredFileScope("UPLOAD")
        public void withScope() {
        }

        public void withoutScope() {
        }
    }

    private HandlerMethod handlerMethodFor(String methodName) throws NoSuchMethodException {
        Method method = DummyController.class.getMethod(methodName);
        return new HandlerMethod(new DummyController(), method);
    }

    @Test
    void rejectsMissingAppIdHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, handlerMethodFor("withScope"));

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void rejectsUnknownOrDisabledApp() throws Exception {
        when(fileAppMapper.selectOne(any())).thenReturn(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-File-App-Id", "ghost-app");
        request.addHeader("X-File-Api-Key", "whatever");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, handlerMethodFor("withScope"));

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void rejectsWrongApiKey() throws Exception {
        FileApp app = new FileApp().setAppId("cms").setApiKeyHash(ApiKeyUtil.sha256Hex("correct-key"))
                .setScopes("UPLOAD").setStatus(1);
        when(fileAppMapper.selectOne(any())).thenReturn(app);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-File-App-Id", "cms");
        request.addHeader("X-File-Api-Key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, handlerMethodFor("withScope"));

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void rejectsMissingScopeAnnotationFailClosed() throws Exception {
        FileApp app = new FileApp().setAppId("cms").setApiKeyHash(ApiKeyUtil.sha256Hex("correct-key"))
                .setScopes("UPLOAD,READ").setStatus(1);
        when(fileAppMapper.selectOne(any())).thenReturn(app);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-File-App-Id", "cms");
        request.addHeader("X-File-Api-Key", "correct-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, handlerMethodFor("withoutScope"));

        assertFalse(allowed);
        assertEquals(403, response.getStatus());
    }

    @Test
    void rejectsInsufficientScope() throws Exception {
        FileApp app = new FileApp().setAppId("cms").setApiKeyHash(ApiKeyUtil.sha256Hex("correct-key"))
                .setScopes("READ").setStatus(1);
        when(fileAppMapper.selectOne(any())).thenReturn(app);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-File-App-Id", "cms");
        request.addHeader("X-File-Api-Key", "correct-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // withScope() 需要 UPLOAD，但该 App 只有 READ
        boolean allowed = interceptor.preHandle(request, response, handlerMethodFor("withScope"));

        assertFalse(allowed);
        assertEquals(403, response.getStatus());
    }

    @Test
    void allowsValidRequestAndPopulatesCallerContext() throws Exception {
        FileApp app = new FileApp().setAppId("cms").setApiKeyHash(ApiKeyUtil.sha256Hex("correct-key"))
                .setScopes("UPLOAD,READ").setStatus(1);
        when(fileAppMapper.selectOne(any())).thenReturn(app);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-File-App-Id", "cms");
        request.addHeader("X-File-Api-Key", "correct-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, handlerMethodFor("withScope"));

        assertTrue(allowed);
        FileCallerContext caller = (FileCallerContext) request.getAttribute(FileAuthInterceptor.CALLER_CONTEXT_ATTR);
        assertNotNull(caller);
        assertEquals("cms", caller.getAppId());
    }
}
