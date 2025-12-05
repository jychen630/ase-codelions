package com.team.mcp.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.team.mcp.auth.TokenStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for GetTokenTool.
 */
class GetTokenToolTest {

  private TokenStore tokenStore;
  private GetTokenTool tool;

  @BeforeEach
  void setUp() {
    tokenStore = mock(TokenStore.class);
    tool = new GetTokenTool(tokenStore);
  }

  @Test
  void name_returnsGetToken() {
    assertEquals("get_token", tool.name());
  }

  @Test
  void description_returnsDescription() {
    assertNotNull(tool.description());
    assertTrue(tool.description().contains("token"));
  }

  @Test
  void call_tokenPresent_returnsTrue() {
    when(tokenStore.get("acct1")).thenReturn(Optional.of("token123"));

    Map<String, Object> args = Map.of("accountId", "acct1");
    List<Map<String, Object>> result = tool.call(args);

    assertNotNull(result);
    assertTrue(result.get(0).get("text").toString().contains("true"));
    verify(tokenStore).get("acct1");
  }

  @Test
  void call_tokenAbsent_returnsFalse() {
    when(tokenStore.get("acct1")).thenReturn(Optional.empty());

    Map<String, Object> args = Map.of("accountId", "acct1");
    List<Map<String, Object>> result = tool.call(args);

    assertNotNull(result);
    assertTrue(result.get(0).get("text").toString().contains("false"));
    verify(tokenStore).get("acct1");
  }

  @Test
  void call_missingAccountId_returnsError() {
    Map<String, Object> args = Map.of();

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
    verifyNoInteractions(tokenStore);
  }

  @Test
  void call_nullAccountId_returnsError() {
    Map<String, Object> args = new java.util.HashMap<>();
    args.put("accountId", null);

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
    verifyNoInteractions(tokenStore);
  }

  @Test
  void call_blankAccountId_returnsError() {
    Map<String, Object> args = Map.of("accountId", "   ");

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
    verifyNoInteractions(tokenStore);
  }

  @Test
  void call_nonStringAccountId_returnsError() {
    Map<String, Object> args = Map.of("accountId", 123);

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
    verifyNoInteractions(tokenStore);
  }
}

