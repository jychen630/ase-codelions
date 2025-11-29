package com.team.mcp.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Logs each HTTP request path and duration.
 * Does not persist; DB audit happens in McpService.
 */
@Component
public final class WebRequestLogFilter extends OncePerRequestFilter {

  /**
   * Class-local logger used for request timing lines.
   */
  private static final Logger LOG =
      LoggerFactory.getLogger(WebRequestLogFilter.class);

  @Override
  protected void doFilterInternal(final HttpServletRequest request,
      final HttpServletResponse response, final FilterChain chain)
      throws ServletException, IOException {
    final long start = System.currentTimeMillis();
    try {
      chain.doFilter(request, response);
    } finally {
      final long ms = System.currentTimeMillis() - start;
      LOG.info("http {} {} -> {} ({} ms)",
          request.getMethod(), request.getRequestURI(),
          response.getStatus(), ms);
    }
  }
}
