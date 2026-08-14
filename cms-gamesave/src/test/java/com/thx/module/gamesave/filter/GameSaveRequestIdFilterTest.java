package com.thx.module.gamesave.filter;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GameSaveRequestIdFilterTest {

    private final GameSaveRequestIdFilter filter = new GameSaveRequestIdFilter();

    @Test
    void validRequestIdShouldBePreservedAndMdcCleared() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader(GameSaveRequestIdFilter.HEADER, "client-request:123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("client-request:123", response.getHeader(GameSaveRequestIdFilter.HEADER));
        assertFalse(MDC.getCopyOfContextMap() != null
                && MDC.getCopyOfContextMap().containsKey("requestId"));
    }

    @Test
    void invalidRequestIdShouldBeReplaced() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader(GameSaveRequestIdFilter.HEADER, "contains spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String generated = response.getHeader(GameSaveRequestIdFilter.HEADER);
        assertNotNull(generated);
        assertFalse(generated.contains(" "));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/game-save/v1/games");
        request.setRequestURI("/api/game-save/v1/games");
        return request;
    }
}
