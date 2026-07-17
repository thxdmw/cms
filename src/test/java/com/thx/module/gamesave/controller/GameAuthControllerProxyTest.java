package com.thx.module.gamesave.controller;

import com.thx.module.gamesave.config.GameSaveProperties;
import com.thx.module.gamesave.service.GameAuthRateLimitService;
import com.thx.module.gamesave.service.GameAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GameAuthControllerProxyTest {

    @Test
    void untrustedCallerCannotSpoofForwardedFor() throws Exception {
        GameSaveProperties properties = new GameSaveProperties();
        GameAuthController controller = controller(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.20");
        request.addHeader("X-Forwarded-For", "203.0.113.9");

        assertEquals("198.51.100.20", clientIp(controller, request));
    }

    @Test
    void trustedProxyCanSupplyForwardedFor() throws Exception {
        GameSaveProperties properties = new GameSaveProperties();
        properties.setTrustedProxyAddresses(Collections.singletonList("10.0.0.10"));
        GameAuthController controller = controller(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.10");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.10");

        assertEquals("203.0.113.9", clientIp(controller, request));
    }

    private GameAuthController controller(GameSaveProperties properties) {
        return new GameAuthController(mock(GameAuthService.class),
                mock(GameAuthRateLimitService.class), properties);
    }

    private String clientIp(GameAuthController controller, MockHttpServletRequest request) throws Exception {
        Method method = GameAuthController.class.getDeclaredMethod(
                "clientIp", javax.servlet.http.HttpServletRequest.class);
        method.setAccessible(true);
        return (String) method.invoke(controller, request);
    }
}
