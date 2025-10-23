package com.team.mcp.audit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class WebRequestLogFilterTest {

  @Test
  void filter_runsAndLogs_noExceptions() {
    WebRequestLogFilter filter = new WebRequestLogFilter();
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/health");
    MockHttpServletResponse res = new MockHttpServletResponse();

    FilterChain chain = (ServletRequest request, ServletResponse response) -> {
      // nothing; just simulate downstream
    };

    assertDoesNotThrow(() -> filter.doFilter(req, res, chain));
  }
}
