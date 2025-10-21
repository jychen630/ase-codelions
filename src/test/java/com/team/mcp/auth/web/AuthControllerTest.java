package com.team.mcp.auth.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.team.mcp.auth.TokenStore;
import com.team.mcp.auth.TwitterOAuthClient;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class AuthControllerTest {

  private TwitterOAuthClient oauth;
  private TokenStore tokenStore;
  private AuthController controller;

  @BeforeEach
  void setUp() {
    oauth = mock(TwitterOAuthClient.class);
    tokenStore = mock(TokenStore.class);
    controller = new AuthController(oauth, tokenStore);
  }

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

  @Test
  void callback_happyPath_storesToken_andReturnsOk() {
    // First call /auth/start to get a valid state
    when(oauth.buildAuthorizeUrl(anyString())).thenReturn("http://auth/demo");
    var start = controller.start("acctA");
    String state = (String) start.getBody().get("state");

    // Then callback exchanges code -> token and stores it
    when(oauth.exchangeCodeForAccessToken("code-123")).thenReturn("tok_abc");

    var resp = controller.callback(state, "code-123");

    assertEquals(200, resp.getStatusCode().value());
    assertEquals("ok", resp.getBody().get("status"));
    assertEquals("acctA", resp.getBody().get("accountId"));
    assertEquals(Boolean.TRUE, resp.getBody().get("stored"));

    verify(oauth).exchangeCodeForAccessToken("code-123");
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
}
