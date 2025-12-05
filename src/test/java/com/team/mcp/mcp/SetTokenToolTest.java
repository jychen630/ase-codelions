package com.team.mcp.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.team.mcp.auth.TokenStore;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SetTokenTool.
 */
class SetTokenToolTest {

  private TokenStore tokenStore;
  private SetTokenTool tool;

  @BeforeEach
  void setUp() {
    tokenStore = mock(TokenStore.class);
    tool = new SetTokenTool(tokenStore);
  }

  @Test
  void name_returnsSetToken() {
    assertEquals("set_token", tool.name());
  }

  @Test
  void description_returnsDescription() {
    assertNotNull(tool.description());
    assertTrue(tool.description().contains("Store"));
  }

  @Test
  void call_validArgs_storesToken() {
    Map<String, Object> args = Map.of(
        "accountId", "acct1",
        "token", "token123"
    );

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("OK", result.get(0).get("text").toString().substring(0, 2));
    verify(tokenStore).put("acct1", "token123");
  }

  @Test
  void call_missingAccountId_returnsError() {
    Map<String, Object> args = Map.of("token", "token123");

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
    verifyNoInteractions(tokenStore);
  }

  @Test
  void call_missingToken_returnsError() {
    Map<String, Object> args = Map.of("accountId", "acct1");

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
    verifyNoInteractions(tokenStore);
  }

  @Test
  void call_nullAccountId_returnsError() {
    Map<String, Object> args = new java.util.HashMap<>();
    args.put("accountId", null);
    args.put("token", "token123");

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
    verifyNoInteractions(tokenStore);
  }

  @Test
  void call_blankAccountId_returnsError() {
    Map<String, Object> args = Map.of(
        "accountId", "   ",
        "token", "token123"
    );

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
    verifyNoInteractions(tokenStore);
  }

  @Test
  void call_blankToken_returnsError() {
    Map<String, Object> args = Map.of(
        "accountId", "acct1",
        "token", "   "
    );

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
    verifyNoInteractions(tokenStore);
  }

  @Test
  void call_nonStringAccountId_returnsError() {
    Map<String, Object> args = Map.of(
        "accountId", 123,
        "token", "token123"
    );

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
    verifyNoInteractions(tokenStore);
  }

  @Test
  void call_nonStringToken_returnsError() {
    Map<String, Object> args = Map.of(
        "accountId", "acct1",
        "token", 123
    );

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
    verifyNoInteractions(tokenStore);
  }
}

