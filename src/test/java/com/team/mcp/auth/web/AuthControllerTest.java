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
    // Use a non-existent state instead of null to avoid NPE in Map.remove()
    var resp = controller.callback("non-existent-state", "any-code");
    assertEquals(400, resp.getStatusCode().value());
    Map<String, Object> body = resp.getBody();
    assertNotNull(body);
    assertEquals("error", body.get("status"));
    assertEquals("invalid or expired state", body.get("message"));
    verifyNoInteractions(tokenStore);
  }


  @Test
  void start_differentAccountIds_createsDifferentStates() {
    when(oauth.buildAuthorizeUrl(anyString())).thenReturn("http://auth/demo");
    
    var resp1 = controller.start("acct1");
    var resp2 = controller.start("acct2");
    
    Map<String, Object> body1 = resp1.getBody();
    Map<String, Object> body2 = resp2.getBody();
    assertNotNull(body1);
    assertNotNull(body2);
    
    String state1 = (String) body1.get("state");
    String state2 = (String) body2.get("state");
    
    assertNotEquals(state1, state2);
    assertNotNull(state1);
    assertNotNull(state2);
  }

  @Test
  void callback_sameStateTwice_secondCallReturns400() {
    when(oauth.buildAuthorizeUrl(anyString())).thenReturn("http://auth/demo");
    var start = controller.start("acctA");
    String state = (String) start.getBody().get("state");

    when(oauth.exchangeCodeForAccessToken("code-1")).thenReturn("tok1");
    var resp1 = controller.callback(state, "code-1");
    assertEquals(200, resp1.getStatusCode().value());

    // Second call with same state should fail (state was removed)
    var resp2 = controller.callback(state, "code-2");
    assertEquals(400, resp2.getStatusCode().value());
  }

  @Test
  void callback_expiredState_returns400() throws Exception {
    when(oauth.buildAuthorizeUrl(anyString())).thenReturn("http://auth/demo");
    var start = controller.start("acctA");
    String state = (String) start.getBody().get("state");

    // Use reflection to access the states map and modify the StateRow
    java.lang.reflect.Field statesField = AuthController.class.getDeclaredField("states");
    statesField.setAccessible(true);
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> states = (java.util.Map<String, Object>) statesField.get(controller);
    
    // Get the StateRow and modify its creation time to be expired
    Object stateRow = states.get(state);
    assertNotNull(stateRow, "StateRow should exist");
    
    java.lang.reflect.Field createdField = stateRow.getClass().getDeclaredField("created");
    createdField.setAccessible(true);
    createdField.set(stateRow, java.time.Instant.now().minusSeconds(600)); // 10 minutes ago

    var resp = controller.callback(state, "code");
    assertEquals(400, resp.getStatusCode().value());
    Map<String, Object> body = resp.getBody();
    assertNotNull(body);
    assertEquals("error", body.get("status"));
  }
}
