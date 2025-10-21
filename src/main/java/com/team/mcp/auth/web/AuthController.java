package com.team.mcp.auth.web;

import com.team.mcp.auth.TwitterOAuthClient;
import com.team.mcp.auth.TokenStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Iteration-1 OAuth flow controller.
 *
 * <p>Endpoints:
 * <ul>
 *   <li><b>GET /auth/start</b> – returns a JSON object with an
 *       authorization URL (simulated). Call this first.</li>
 *   <li><b>GET /auth/callback</b> – simulated callback that exchanges
 *       a code for a token and persists it via {@link TokenStore}.</li>
 * </ul>
 *
 * <p>Notes:
 * <ul>
 *   <li>No real Twitter calls yet; {@link TwitterOAuthClient} is a stub.</li>
 *   <li>State is stored in-memory with a short TTL for the demo.</li>
 * </ul>
 */
@RestController
@RequestMapping(
    path = "/auth",
    produces = MediaType.APPLICATION_JSON_VALUE)
public final class AuthController {

  /** Time-to-live for saved state entries. */
  private static final Duration STATE_TTL = Duration.ofMinutes(10);

  /** In-memory map: state -> (accountId, createdAt). */
  private final Map<String, StateRow> states = new ConcurrentHashMap<>();

  /** Stub OAuth client used to build URLs and mint fake tokens. */
  private final TwitterOAuthClient oauth;

  /** Token persistence (DB-backed in iteration-1). */
  private final TokenStore tokenStore;

  /**
   * Creates the controller.
   *
   * @param oauthClient stub OAuth client
   * @param store token persistence
   */
  public AuthController(
      final TwitterOAuthClient oauthClient,
      final TokenStore store) {
    this.oauth = Objects.requireNonNull(oauthClient, "oauth");
    this.tokenStore = Objects.requireNonNull(store, "tokenStore");
  }

  /**
   * Begin the OAuth flow. Returns a JSON body with an
   * {@code authorize_url} you can paste in a browser
   * (simulated for iteration-1).
   *
   * @param accountId logical account id / tenant id
   * @return JSON with {@code authorize_url}, {@code state}, and
   *     {@code callback}
   */
  @GetMapping("/start")
  public ResponseEntity<Map<String, Object>> start(
      @RequestParam("accountId") final String accountId) {
    final String state = UUID.randomUUID().toString();
    states.put(state, new StateRow(accountId, Instant.now()));
    final String url = oauth.buildAuthorizeUrl(state);
    return ResponseEntity.ok(Map.of(
        "authorize_url", url,
        "state", state,
        "callback", TwitterOAuthClient.CALLBACK));
  }

  /**
   * OAuth callback endpoint (simulated).
   *
   * @param state original CSRF state
   * @param code authorization code
   * @return JSON result message
   */
  @GetMapping("/callback")
  public ResponseEntity<Map<String, Object>> callback(
      @RequestParam("state") final String state,
      @RequestParam("code") final String code) {

    final StateRow row = states.remove(state);
    if (row == null || isExpired(row)) {
      return ResponseEntity.badRequest().body(Map.of(
          "status", "error",
          "message", "invalid or expired state"));
    }

    final String token = oauth.exchangeCodeForAccessToken(code);
    tokenStore.put(row.accountId, token);

    return ResponseEntity.ok(Map.of(
        "status", "ok",
        "accountId", row.accountId,
        "stored", Boolean.TRUE));
  }

  /**
   * Returns whether a stored state row has expired.
   *
   * @param row state row to check
   * @return {@code true} if expired; otherwise {@code false}
   */
  private boolean isExpired(final StateRow row) {
    return row.created.plus(STATE_TTL).isBefore(Instant.now());
  }

  /** Small immutable record for state storage. */
  private static final class StateRow {
    /** Logical account id / tenant id. */
    private final String accountId;
    /** Creation instant (UTC). */
    private final Instant created;

    /**
     * Constructs a state row.
     *
     * @param accId logical account id
     * @param createdAt creation instant
     */
    private StateRow(final String accId, final Instant createdAt) {
      this.accountId = accId;
      this.created = createdAt;
    }
  }
}
