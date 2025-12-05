package com.team.mcp.audit;

import com.team.mcp.security.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that logs API calls into {@code api_logs}.
 *
 * <p>For Iteration-2 it focuses on the main JSON tools/search endpoints:
 * <ul>
 *   <li>/tools/**</li>
 *   <li>/search</li>
 *   <li>/search/hashtags</li>
 * </ul>
 */
@Profile("!test")
@Component
public final class ApiLoggingFilter extends OncePerRequestFilter {

  /** Logger for best-effort logging of failures. */
  private static final Logger LOG =
      LoggerFactory.getLogger(ApiLoggingFilter.class);

  /** Repository used to persist log rows. */
  private final ApiLogRepository repo;

  /** Optional token provider for resolving logical account ids. */
  private TokenProvider tokenProvider;

  /**
   * Primary constructor used by Spring.
   *
   * @param repoParam repository for saving log rows
   */
  public ApiLoggingFilter(final ApiLogRepository repoParam) {
    this.repo = repoParam;
  }

  /**
   * Optional setter injection for {@link TokenProvider}.
   * Keeps constructor stable even if security wiring changes.
   *
   * @param tokenProviderParam token provider bean (may be null)
   */
  @Autowired(required = false)
  public void setTokenProvider(final TokenProvider tokenProviderParam) {
    this.tokenProvider = tokenProviderParam;
  }

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {

    final String tool = resolveTool(request);
    // Only log "API" endpoints we care about; skip others.
    if (tool == null) {
      filterChain.doFilter(request, response);
      return;
    }

    final String accountId = resolveAccountId(request);
    final String paramsHash = hashParams(request);

    int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    String errorMsg = null;

    try {
      filterChain.doFilter(request, response);
      status = response.getStatus();
    } catch (Exception ex) {
      status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
      errorMsg = ex.getMessage();
      throw ex;
    } finally {
      final ApiLog row =
          new ApiLog(tool, accountId, paramsHash, status, errorMsg);
      // Best-effort: logging failure should not break the request.
      try {
        repo.save(row);
      } catch (Exception ex) {
        LOG.debug("Failed to persist ApiLog row: {}", ex.getMessage(), ex);
      }
    }
  }

  private static String resolveTool(final HttpServletRequest request) {
    final String path = request.getRequestURI();
    if (path == null) {
      return null;
    }
    if (path.startsWith("/tools/")) {
      return path.substring("/tools/".length());
    }
    if ("/search".equals(path)) {
      return "search";
    }
    if ("/search/hashtags".equals(path)) {
      return "search_hashtags";
    }
    return null;
  }

  private String resolveAccountId(final HttpServletRequest request) {
    if (tokenProvider != null) {
      try {
        final String fromToken = tokenProvider.accountIdForCaller();
        if (fromToken != null && !fromToken.isBlank()) {
          return fromToken;
        }
      } catch (Exception ignored) {
        // fall through
      }
    }

    final String param = request.getParameter("accountId");
    if (param != null && !param.isBlank()) {
      return param;
    }

    final String header = request.getHeader("X-Account-Id");
    if (header != null && !header.isBlank()) {
      return header;
    }

    return null;
  }

  private static String hashParams(final HttpServletRequest request) {
    final StringBuilder sb = new StringBuilder();
    sb.append(request.getMethod())
        .append(' ')
        .append(request.getRequestURI());

    final String qs = request.getQueryString();
    if (qs != null) {
      sb.append('?').append(qs);
    }

    final Map<String, String[]> params = request.getParameterMap();
    params.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(e -> sb.append('|')
            .append(e.getKey())
            .append('=')
            .append(Arrays.toString(e.getValue())));

    final String raw = sb.toString();
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      final byte[] digest =
          md.digest(raw.getBytes(StandardCharsets.UTF_8));
      final StringBuilder hex = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException ex) {
      return Integer.toHexString(raw.hashCode());
    }
  }
}

