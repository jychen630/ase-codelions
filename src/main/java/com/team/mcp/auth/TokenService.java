package com.team.mcp.auth;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Minimal in-memory token store for Iteration-1.
 *
 * <p>Provides a tiny API that MCP tools can use (independent from the
 * database-backed {@code TokenStore}). You can swap this out later
 * without changing tool code.
 */
@Service
public class TokenService {

  /** Number of tail characters to show for redaction. */
  private static final int LAST_N = 4;

  /**
   * Redacted view returned by {@link #getToken(String, String)}.
   *
   * @param accessLast4 last 4 chars of access token (or shorter/empty)
   * @param hasRefresh whether a refresh token is present
   * @param expiresAtIso ISO-8601 expiry time, or {@code null} if not set
   * @param scopesCsv comma-separated scopes, or {@code null}
   */
  public record TokenView(
      String accessLast4,
      boolean hasRefresh,
      String expiresAtIso,
      String scopesCsv) {
  }

  /** Internal stored token. */
  private static final class Stored {

    /** Plain access token (demo only). */
    private final String access;
    /** Plain refresh token (optional, demo only). */
    private final String refresh;
    /** Expiry moment (optional). */
    private final Instant expiresAt;
    /** Comma-separated scopes (optional). */
    private final String scopes;

    Stored(final String accessToken,
           final String refreshToken,
           final Instant expires,
           final String scopesCsv) {
      this.access = accessToken;
      this.refresh = refreshToken;
      this.expiresAt = expires;
      this.scopes = scopesCsv;
    }

    String access() {
      return access;
    }

    String refresh() {
      return refresh;
    }

    Instant expiresAt() {
      return expiresAt;
    }

    String scopes() {
      return scopes;
    }
  }

  /** In-memory map keyed by "account|provider". */
  private final Map<String, Stored> store = new ConcurrentHashMap<>();

  private static String key(final String accountId, final String provider) {
    final String prov = provider == null || provider.isBlank()
        ? "twitter"
        : provider;
    return accountId + "|" + prov;
  }

  /**
   * Create or update a token for an account+provider.
   *
   * @param accountId logical account id
   * @param provider provider id (e.g., {@code "twitter"});
   *                 defaults to {@code "twitter"} if blank
   * @param access plaintext access token
   * @param refresh plaintext refresh token (optional)
   * @param expiresAt expiry (optional)
   * @param scopes comma-separated scopes (optional)
   * @throws IllegalArgumentException if accountId is blank
   */
  public void upsertToken(final String accountId,
                          final String provider,
                          final String access,
                          final String refresh,
                          final Instant expiresAt,
                          final String scopes) {
    if (accountId == null || accountId.isBlank()) {
      throw new IllegalArgumentException("account_id required");
    }
    final String prov = provider == null || provider.isBlank()
        ? "twitter"
        : provider;
    store.put(key(accountId, prov),
        new Stored(access, refresh, expiresAt, scopes));
  }

  /**
   * Return a redacted view of the stored token (or {@code null} if none).
   *
   * @param accountId account id
   * @param provider provider id; defaults to {@code "twitter"} if blank
   * @return TokenView or {@code null}
   */
  public TokenView getToken(final String accountId,
                            final String provider) {
    final String prov = provider == null || provider.isBlank()
        ? "twitter"
        : provider;
    final Stored s = store.get(key(accountId, prov));
    if (s == null) {
      return null;
    }

    final String access = s.access();
    final String last4;
    if (access == null || access.isEmpty()) {
      last4 = "";
    } else if (access.length() <= LAST_N) {
      last4 = access;
    } else {
      last4 = access.substring(access.length() - LAST_N);
    }

    final boolean hasRefresh =
        s.refresh() != null && !s.refresh().isBlank();
    final String expiresIso =
        s.expiresAt() == null ? null : s.expiresAt().toString();

    return new TokenView(last4, hasRefresh, expiresIso, s.scopes());
  }
}
