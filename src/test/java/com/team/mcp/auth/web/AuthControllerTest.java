package com.team.mcp.auth.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.team.mcp.auth.TokenService;
import com.team.mcp.auth.TokenStore;
import com.team.mcp.auth.TwitterOAuthClient;
import java.lang.reflect.Constructor;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link AuthController}.
 *
 * We construct the controller directly (no Spring context) and
 * exercise the main flows plus the Iteration-2 status/admin helpers.
 */
class AuthControllerTest {

  private TwitterOAuthClient oauth;
  private TokenStore tokenStore;
  private AuthController controller;

  @BeforeEach
  void setUp() {
    oauth = mock(TwitterOAuthClient.class);
    tokenStore = mock(TokenStore.class);
    // Use the 2-arg constructor; tokenService will be null
    controller = new AuthController(oauth, tokenStore);
    // Simulate @Value injection used in production
    ReflectionTestUtils.setField(
        controller,
        "configuredScopes",
        "read:statuses write:statuses read:accounts");
  }

  // ---------------------------------------------------------------------------
  // /auth/start
  // ---------------------------------------------------------------------------

  @Test
  void start_returnsAuthorizeUrlAndState_andCallback() {
    when(oauth.buildAuthorizeUrl(anyString())).thenReturn("http://auth/demo");

    ResponseEntity<Map<String, Object>> resp = controller.start("acctA");

    assertEquals(200, resp.getStatusCode().value());
    Map<String, Object> body = resp.getBody();
    assertNotNull(body);
    assertEquals("http://auth/demo", body.get("authorize_url"));
    assertTrue(body.containsKey("state"));
    assertNotNull(body.get("state"));
    assertEquals(TwitterOAuthClient.CALLBACK, body.get("callback"));

    verify(oauth).buildAuthorizeUrl(anyString());
    verifyNoInteractions(tokenStore);
  }

  // ---------------------------------------------------------------------------
  // /auth/callback
  // ---------------------------------------------------------------------------

  @Test
  void callback_happyPath_storesToken_andReturnsOk() {
    // First call /auth/start to get a valid state
    when(oauth.buildAuthorizeUrl(anyString())).thenReturn("http://auth/demo");
    var start = controller.start("acctA");
    String state = (String) start.getBody().get("state");

    // In Iteration-2, AuthController uses exchangeWithMetadata(...)
    TwitterOAuthClient.OAuthTokens tokens =
        new TwitterOAuthClient.OAuthTokens(
            "tok_abc", null, 3600L, "read:statuses");
    when(oauth.exchangeWithMetadata("code-123")).thenReturn(tokens);

    var resp = controller.callback(state, "code-123");

    assertEquals(200, resp.getStatusCode().value());
    assertEquals("ok", resp.getBody().get("status"));
    assertEquals("acctA", resp.getBody().get("accountId"));
    assertEquals(Boolean.TRUE, resp.getBody().get("stored"));

    // Verify we used the metadata-style exchange and persisted the access token
    verify(oauth).exchangeWithMetadata("code-123");
    verify(tokenStore).put("acctA", "tok_abc");
  }

  @Test
  void callback_invalidState_returns400() {
    var resp = controller.callback("no-such-state", "any-code");
    assertEquals(400, resp.getStatusCode().value());
    assertEquals("error", resp.getBody().get("status"));
    assertEquals("invalid or expired state", resp.getBody().get("message"));
    verifyNoInteractions(tokenStore);
  }

  @Test
  void callback_expiredState_returns400() throws Exception {
    // Manually insert an expired StateRow into the private "states" map
    Instant oldTime = Instant.now().minus(Duration.ofMinutes(20));

    Class<?> rowClass =
        Class.forName("com.team.mcp.auth.web.AuthController$StateRow");
    Constructor<?> ctor =
        rowClass.getDeclaredConstructor(String.class, Instant.class);
    ctor.setAccessible(true);
    Object row = ctor.newInstance("acctA", oldTime);

    @SuppressWarnings("unchecked")
    Map<String, Object> states =
        (Map<String, Object>) ReflectionTestUtils.getField(controller, "states");
    states.put("expired-state", row);

    var resp = controller.callback("expired-state", "code-xyz");
    assertEquals(400, resp.getStatusCode().value());
    assertEquals("error", resp.getBody().get("status"));
    assertEquals("invalid or expired state", resp.getBody().get("message"));

    verifyNoInteractions(tokenStore);
    verifyNoInteractions(oauth);
  }

  // ---------------------------------------------------------------------------
  // /auth/status  – without TokenService (fallback to TokenStore)
  // ---------------------------------------------------------------------------

  @Test
  void status_withoutTokenService_hasToken_unknownHealth() {
    when(tokenStore.get("acctA")).thenReturn(Optional.of("tok123"));

    Map<String, Object> body = controller.status("acctA");

    assertEquals("acctA", body.get("accountId"));
    assertEquals(true, body.get("hasToken"));
    assertEquals("mastodon", body.get("provider"));
    assertEquals(
        "read:statuses write:statuses read:accounts",
        body.get("scopes"));
    assertNull(body.get("hasRefresh"));
    assertNull(body.get("expiresAt"));
    assertEquals("unknown", body.get("health"));
  }

  @Test
  void status_withoutTokenService_noToken_missingHealth() {
    when(tokenStore.get("acctA")).thenReturn(Optional.empty());

    Map<String, Object> body = controller.status("acctA");

    assertEquals("acctA", body.get("accountId"));
    assertEquals(false, body.get("hasToken"));
    assertNull(body.get("provider"));
    assertNull(body.get("scopes"));
    assertNull(body.get("hasRefresh"));
    assertNull(body.get("expiresAt"));
    assertEquals("missing", body.get("health"));
  }

  // ---------------------------------------------------------------------------
  // /auth/status – with TokenService (full metadata + classifyHealth branches)
  // ---------------------------------------------------------------------------

  @Test
  void status_withTokenService_missingView_reportsMissing() {
    TokenService tokenService = mock(TokenService.class);
    AuthController c = new AuthController(oauth, tokenStore, tokenService);

    when(tokenService.getToken("acctA", "mastodon")).thenReturn(null);

    Map<String, Object> body = c.status("acctA");

    assertEquals("acctA", body.get("accountId"));
    assertEquals(false, body.get("hasToken"));
    assertEquals("missing", body.get("health"));
    assertNull(body.get("provider"));
    assertNull(body.get("scopes"));
    assertEquals(false, body.get("hasRefresh"));
    assertNull(body.get("expiresAt"));

    verify(tokenService).getToken("acctA", "mastodon");
    verifyNoInteractions(tokenStore);
  }

  @Test
  void status_withTokenService_healthyToken() {
    TokenService tokenService = mock(TokenService.class);
    AuthController c = new AuthController(oauth, tokenStore, tokenService);

    TokenService.TokenView view = mock(TokenService.TokenView.class);
    Instant future = Instant.now().plusSeconds(7200); // > 1h ahead

    when(view.scopesCsv()).thenReturn("s1");
    when(view.hasRefresh()).thenReturn(true);
    when(view.expiresAtIso()).thenReturn(future.toString());
    when(tokenService.getToken("acctA", "mastodon")).thenReturn(view);

    Map<String, Object> body = c.status("acctA");

    assertEquals(true, body.get("hasToken"));
    assertEquals("mastodon", body.get("provider"));
    assertEquals("s1", body.get("scopes"));
    assertEquals(true, body.get("hasRefresh"));
    assertEquals(future.toString(), body.get("expiresAt"));
    assertEquals("healthy", body.get("health"));

    verifyNoInteractions(tokenStore);
  }

  @Test
  void status_withTokenService_expiringSoonToken() {
    TokenService tokenService = mock(TokenService.class);
    AuthController c = new AuthController(oauth, tokenStore, tokenService);

    TokenService.TokenView view = mock(TokenService.TokenView.class);
    Instant soon = Instant.now().plusSeconds(600); // within 1h

    when(view.scopesCsv()).thenReturn("s1");
    when(view.hasRefresh()).thenReturn(false);
    when(view.expiresAtIso()).thenReturn(soon.toString());
    when(tokenService.getToken("acctA", "mastodon")).thenReturn(view);

    Map<String, Object> body = c.status("acctA");

    assertEquals(true, body.get("hasToken"));
    assertEquals("expiring-soon", body.get("health"));
    assertEquals(false, body.get("hasRefresh"));
  }

  @Test
  void status_withTokenService_expiredToken() {
    TokenService tokenService = mock(TokenService.class);
    AuthController c = new AuthController(oauth, tokenStore, tokenService);

    TokenService.TokenView view = mock(TokenService.TokenView.class);
    Instant past = Instant.now().minusSeconds(60); // already expired

    when(view.scopesCsv()).thenReturn("s1");
    when(view.hasRefresh()).thenReturn(true);
    when(view.expiresAtIso()).thenReturn(past.toString());
    when(tokenService.getToken("acctA", "mastodon")).thenReturn(view);

    Map<String, Object> body = c.status("acctA");

    assertEquals(true, body.get("hasToken"));
    assertEquals("expired", body.get("health"));
  }

  @Test
  void status_withTokenService_invalidExpiry_reportsUnknown() {
    TokenService tokenService = mock(TokenService.class);
    AuthController c = new AuthController(oauth, tokenStore, tokenService);

    TokenService.TokenView view = mock(TokenService.TokenView.class);

    when(view.scopesCsv()).thenReturn("s1");
    when(view.hasRefresh()).thenReturn(true);
    when(view.expiresAtIso()).thenReturn("not-a-timestamp");
    when(tokenService.getToken("acctA", "mastodon")).thenReturn(view);

    Map<String, Object> body = c.status("acctA");

    assertEquals(true, body.get("hasToken"));
    assertEquals("unknown", body.get("health"));
  }

  @Test
  void status_withTokenService_nullExpiry_reportsUnknown() {
    TokenService tokenService = mock(TokenService.class);
    AuthController c = new AuthController(oauth, tokenStore, tokenService);

    TokenService.TokenView view = mock(TokenService.TokenView.class);

    when(view.scopesCsv()).thenReturn("s1");
    when(view.hasRefresh()).thenReturn(true);
    when(view.expiresAtIso()).thenReturn(null);
    when(tokenService.getToken("acctA", "mastodon")).thenReturn(view);

    Map<String, Object> body = c.status("acctA");

    assertEquals(true, body.get("hasToken"));
    assertEquals("unknown", body.get("health"));
  }

  // ---------------------------------------------------------------------------
  // /auth/token (DELETE)
  // ---------------------------------------------------------------------------

  @Test
  void deleteToken_withoutTokenService_clearsStoreOnly() {
    Map<String, Object> body = controller.deleteToken("acctA");

    assertEquals("acctA", body.get("accountId"));
    assertEquals("deleted", body.get("status"));

    verify(tokenStore).put("acctA", "");
  }

  @Test
  void deleteToken_withTokenService_clearsBoth() {
    TokenService tokenService = mock(TokenService.class);
    AuthController c = new AuthController(oauth, tokenStore, tokenService);

    Map<String, Object> body = c.deleteToken("acctA");

    assertEquals("acctA", body.get("accountId"));
    assertEquals("deleted", body.get("status"));

    verify(tokenService).clearToken("acctA", "mastodon");
    verify(tokenStore).put("acctA", "");
  }

  // ---------------------------------------------------------------------------
  // /auth/meta
  // ---------------------------------------------------------------------------

  @Test
  void meta_returnsProviderCallbackAndScopes() {
    Map<String, Object> body = controller.meta();

    assertEquals("mastodon", body.get("provider"));
    assertEquals(TwitterOAuthClient.CALLBACK, body.get("callback"));
    assertEquals(
        "read:statuses write:statuses read:accounts",
        body.get("scopes"));
  }
}

