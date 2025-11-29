package com.team.mcp.auth.web;

import com.team.mcp.auth.TokenService;
import com.team.mcp.auth.TokenStore;
import com.team.mcp.auth.TwitterOAuthClient;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth flow controller plus small token admin endpoints.
 *
 * <p>Main flow:
 * <ul>
 *   <li><b>GET /auth/start</b> – returns a JSON object with an
 *       authorization URL. Call this first.</li>
 *   <li><b>GET /auth/callback</b> – callback that exchanges a
 *       code for an access token and persists it via {@link TokenStore}.</li>
 * </ul>
 *
 * <p>Iteration-2 extras:
 * <ul>
 *   <li><b>GET /auth/status</b> – lightweight status for a given accountId
 *       (does it have a token? what provider/scopes? expiry?).</li>
 *   <li><b>DELETE /auth/token</b> – delete/revoke the stored token for an
 *       accountId (local lifecycle management).</li>
 *   <li><b>GET /auth/meta</b> – static metadata about the auth provider
 *       and callback URL.</li>
 * </ul>
 */
@RestController
@RequestMapping(
    path = "/auth",
    produces = MediaType.APPLICATION_JSON_VALUE)
public final class AuthController {

  /** Time-to-live for saved state entries. */
  private static final Duration STATE_TTL = Duration.ofMinutes(10);

  /** Logical provider label used in status/meta responses. */
  private static final String PROVIDER = "mastodon";

  /** Threshold in seconds to consider a token "expiring soon" (1 hour). */
  private static final long EXPIRING_SOON_THRESHOLD_SECONDS = 3600L;

  /** In-memory map: state -> (accountId, createdAt). */
  private final Map<String, StateRow> states = new ConcurrentHashMap<>();

  /** OAuth client used to build URLs and mint access tokens. */
  private final TwitterOAuthClient oauth;

  /** Token persistence (DB-backed). */
  private final TokenStore tokenStore;

  /** Optional richer in-memory token metadata (Iteration-2). */
  private final TokenService tokenService;

  /**
   * Scopes granted by our Mastodon app.
   * Default matches your application-mastodon.properties.
   */
  @Value("${mastodon.oauth.scopes:read:statuses write:statuses read:accounts}")
  private String configuredScopes;

  /**
   * Creates the controller (greedy constructor for Spring).
   *
   * @param oauthClient OAuth client (Mastodon-backed in Iteration-2)
   * @param store token persistence
   * @param tokenServiceParam richer token metadata service (optional in tests)
   */
  @Autowired
  public AuthController(
      final TwitterOAuthClient oauthClient,
      final TokenStore store,
      final TokenService tokenServiceParam) {
    this.oauth = Objects.requireNonNull(oauthClient, "oauth");
    this.tokenStore = Objects.requireNonNull(store, "tokenStore");
    // may be null in some tests
    this.tokenService = tokenServiceParam;
  }

  /**
   * Backwards-compatible constructor used by older tests that only
   * pass oauth + tokenStore. The TokenService will simply be unused.
   *
   * @param oauthClient OAuth client (Mastodon-backed in Iteration-2)
   * @param store token persistence
   */
  @SuppressWarnings("unused")
  public AuthController(
      final TwitterOAuthClient oauthClient,
      final TokenStore store) {
    this(oauthClient, store, null);
  }

  /**
   * Begin the OAuth flow.
   *
   * <p>Returns a JSON body with an {@code authorize_url} you can paste
   * in a browser, plus a CSRF {@code state} and the {@code callback} URL.
   *
   * @param accountId logical account identifier
   * @return response entity containing authorize URL, state, and callback URL
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
   * OAuth callback endpoint.
   *
   * @param state CSRF state previously issued by {@link #start(String)}
   * @param code authorization code returned by the provider
   * @return response with either {@code status=ok} and stored token info
   *     or an error payload if the state is invalid or expired
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

    // Iteration-2: get richer token metadata from the OAuth client
    final TwitterOAuthClient.OAuthTokens tokens =
        oauth.exchangeWithMetadata(code);

    // Persist the opaque access token in the DB-backed store
    tokenStore.put(row.accountId, tokens.accessToken());

    // Optionally persist metadata (refresh, expiry, scopes) in memory
    if (tokenService != null) {
      Instant expiresAt = null;
      if (tokens.expiresIn() != null && tokens.expiresIn() > 0) {
        expiresAt = Instant.now().plusSeconds(tokens.expiresIn());
      }
      tokenService.upsertToken(
          row.accountId,
          PROVIDER,
          tokens.accessToken(),
          tokens.refreshToken(),
          expiresAt,
          tokens.scope());
    }

    return ResponseEntity.ok(Map.of(
        "status", "ok",
        "accountId", row.accountId,
        "stored", Boolean.TRUE));
  }

  // ---------------------------------------------------------------------------
  // Iteration-2: small admin / lifecycle endpoints
  // ---------------------------------------------------------------------------

  /**
   * Lightweight token status endpoint.
   *
   * <p>Returns whether an account has a token, and if {@link TokenService} is
   * available, additional metadata such as scopes, expiry and health.
   *
   * @param accountId logical account identifier
   * @return a map describing token presence, provider, scopes and health
   */
  @GetMapping("/status")
  public Map<String, Object> status(
      @RequestParam("accountId") final String accountId) {

    final Map<String, Object> body = new LinkedHashMap<>();
    body.put("accountId", accountId);

    // Prefer the richer TokenService if available
    if (tokenService != null) {
      final TokenService.TokenView view =
          tokenService.getToken(accountId, PROVIDER);

      if (view == null) {
        body.put("hasToken", false);
        body.put("provider", null);
        body.put("scopes", null);
        body.put("hasRefresh", false);
        body.put("expiresAt", null);
        body.put("health", "missing");
        return body;
      }

      body.put("hasToken", true);
      body.put("provider", PROVIDER);
      body.put("scopes", view.scopesCsv());
      body.put("hasRefresh", view.hasRefresh());
      body.put("expiresAt", view.expiresAtIso());
      body.put("health", classifyHealth(view.expiresAtIso()));
      return body;
    }

    // Fallback: only know "has token" from TokenStore
    final String token = tokenStore.get(accountId).orElse(null);
    final boolean hasToken = token != null && !token.isBlank();

    body.put("hasToken", hasToken);
    body.put("provider", hasToken ? PROVIDER : null);
    body.put("scopes", hasToken ? configuredScopes : null);
    body.put("hasRefresh", null);
    body.put("expiresAt", null);
    body.put("health", hasToken ? "unknown" : "missing");
    return body;
  }

  /**
   * Delete/revoke the stored token for a given account.
   *
   * <p>Clears both the richer {@link TokenService} metadata (if present)
   * and the DB-backed {@link TokenStore} entry.
   *
   * @param accountId logical account identifier
   * @return a small map confirming deletion
   */
  @DeleteMapping("/token")
  public Map<String, Object> deleteToken(
      @RequestParam("accountId") final String accountId) {

    if (tokenService != null) {
      tokenService.clearToken(accountId, PROVIDER);
    }

    tokenStore.put(accountId, "");

    final Map<String, Object> body = new LinkedHashMap<>();
    body.put("accountId", accountId);
    body.put("status", "deleted");
    return body;
  }

  /**
   * Static metadata about the auth provider and callback.
   *
   * @return a map describing provider id, callback URL and configured scopes
   */
  @GetMapping("/meta")
  public Map<String, Object> meta() {
    final Map<String, Object> body = new LinkedHashMap<>();
    body.put("provider", PROVIDER);
    body.put("callback", TwitterOAuthClient.CALLBACK);
    body.put("scopes", configuredScopes);
    return body;
  }

  // ---------------------------------------------------------------------------
  // Internal helpers
  // ---------------------------------------------------------------------------

  /**
   * Returns whether the given saved state has expired.
   *
   * @param row stored state row
   * @return true if the state's creation time is older than {@link #STATE_TTL}
   */
  private boolean isExpired(final StateRow row) {
    return row.created.plus(STATE_TTL).isBefore(Instant.now());
  }

  /**
   * Classify the health of a token based on an ISO-8601 expiry timestamp.
   *
   * <ul>
   *   <li>{@code expired} – expiry is before now</li>
   *   <li>{@code expiring-soon} – expiry is within the next hour</li>
   *   <li>{@code healthy} – expiry is more than an hour away</li>
   *   <li>{@code unknown} – parsing failed or expiry is missing</li>
   * </ul>
   *
   * @param expiresIso ISO-8601 timestamp for token expiry, or null/blank
   * @return health classification string
   */
  private static String classifyHealth(final String expiresIso) {
    if (expiresIso == null || expiresIso.isBlank()) {
      return "unknown";
    }
    try {
      final Instant expiresAt = Instant.parse(expiresIso);
      final Instant now = Instant.now();
      if (expiresAt.isBefore(now)) {
        return "expired";
      }
      if (expiresAt
          .minusSeconds(EXPIRING_SOON_THRESHOLD_SECONDS)
          .isBefore(now)) {
        return "expiring-soon";
      }
      return "healthy";
    } catch (Exception ex) {
      return "unknown";
    }
  }

  /**
   * Small immutable record for state storage.
   */
  private static final class StateRow {

    /** Logical account id associated with this state. */
    private final String accountId;

    /** Creation time of this state entry. */
    private final Instant created;

    /**
     * Creates a new state row.
     *
     * @param accId logical account identifier
     * @param createdAt creation timestamp for the state
     */
    private StateRow(final String accId, final Instant createdAt) {
      this.accountId = accId;
      this.created = createdAt;
    }
  }
}

