package com.team.mcp.audit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.team.mcp.security.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for {@link ApiLoggingFilter}.
 */
class ApiLoggingFilterTest {

  @Test
  void logsToolsRequests_withStatusAndAccountId() throws Exception {
    ApiLogRepository repo = mock(ApiLogRepository.class);
    ApiLoggingFilter filter = new ApiLoggingFilter(repo);

    TokenProvider tokenProvider = mock(TokenProvider.class);
    when(tokenProvider.accountIdForCaller()).thenReturn("acct-1");
    filter.setTokenProvider(tokenProvider);

    MockHttpServletRequest req =
        new MockHttpServletRequest("POST", "/tools/schedule_tweet");
    req.addParameter("text", "Hello");
    req.addParameter("time", "2025-10-01T12:00:00Z");
    MockHttpServletResponse resp = new MockHttpServletResponse();

    FilterChain chain = (request, response) ->
        ((MockHttpServletResponse) response).setStatus(201);

    filter.doFilter(req, resp, chain);

    ArgumentCaptor<ApiLog> captor = ArgumentCaptor.forClass(ApiLog.class);
    verify(repo).save(captor.capture());
    ApiLog row = captor.getValue();

    assertEquals("schedule_tweet", row.getTool());
    assertEquals("acct-1", row.getAccountId());
    assertEquals(201, row.getStatusCode());
    assertNotNull(row.getParamsHash());
    assertFalse(row.getParamsHash().isBlank());
  }

  @Test
  void skipsNonApiPaths() throws Exception {
    ApiLogRepository repo = mock(ApiLogRepository.class);
    ApiLoggingFilter filter = new ApiLoggingFilter(repo);

    MockHttpServletRequest req =
        new MockHttpServletRequest("GET", "/actuator/health");
    MockHttpServletResponse resp = new MockHttpServletResponse();

    FilterChain chain = (request, response) ->
        ((MockHttpServletResponse) response).setStatus(200);

    filter.doFilter(req, resp, chain);

    verify(repo, never()).save(any());
  }

  @Test
  void logsExceptionWith500AndMessage() throws IOException, ServletException {
    ApiLogRepository repo = mock(ApiLogRepository.class);
    ApiLoggingFilter filter = new ApiLoggingFilter(repo);

    MockHttpServletRequest req =
        new MockHttpServletRequest("GET", "/tools/get_home_timeline");
    MockHttpServletResponse resp = new MockHttpServletResponse();

    FilterChain chain = (request, response) -> {
      throw new RuntimeException("boom");
    };

    try {
      filter.doFilter(req, resp, chain);
      fail("Expected RuntimeException to be rethrown");
    } catch (RuntimeException ex) {
      // expected
    }

    ArgumentCaptor<ApiLog> captor = ArgumentCaptor.forClass(ApiLog.class);
    verify(repo).save(captor.capture());
    ApiLog row = captor.getValue();

    assertEquals("get_home_timeline", row.getTool());
    assertEquals(500, row.getStatusCode());
    assertNotNull(row.getErrorMsg());
    assertTrue(row.getErrorMsg().toLowerCase().contains("boom"));
  }
}

