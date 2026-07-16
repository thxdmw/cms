package com.thx.module.gamesave.interceptor;

import com.thx.module.gamesave.config.GameSaveProperties;
import com.thx.module.gamesave.mapper.GameDeviceMapper;
import com.thx.module.gamesave.model.GameDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameDeviceTokenInterceptorTest {

    @Mock
    private GameDeviceMapper mapper;
    private GameDeviceTokenInterceptor interceptor;

    @BeforeEach
    void setUp() {
        GameSaveProperties properties = new GameSaveProperties();
        properties.setLastSeenUpdateMinutes(10);
        interceptor = new GameDeviceTokenInterceptor(mapper, properties);
    }

    @Test
    void expiredTokenShouldReturnStableCode() throws Exception {
        when(mapper.selectOne(any())).thenReturn(device(new Date(System.currentTimeMillis() - 1000)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request(), response, new Object());

        assertFalse(allowed);
        assertTrue(response.getContentAsString().contains("\"code\":\"TOKEN_EXPIRED\""));
    }

    @Test
    void lastSeenUpdateFailureShouldNotBreakAuthenticatedRequest() throws Exception {
        when(mapper.selectOne(any())).thenReturn(device(new Date(System.currentTimeMillis() + 60_000)));
        when(mapper.touchLastSeenIfStale(any(), any(), any()))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertTrue(interceptor.preHandle(request(), new MockHttpServletResponse(), new Object()));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        return request;
    }

    private GameDevice device(Date expiresAt) {
        return new GameDevice()
                .setId(1L)
                .setUserId("user-1")
                .setDeviceId("device-1")
                .setTokenExpireTime(expiresAt)
                .setStatus(1);
    }
}
