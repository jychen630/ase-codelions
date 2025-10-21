package com.team.mcp.mcp;

import static org.junit.jupiter.api.Assertions.*;

import com.team.mcp.auth.TokenStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for token utility tools using an in-memory stub TokenStore.
 */
class TokenToolsTest {

  /** Minimal in-memory TokenStore that matches com.team.mcp.auth.TokenStore. */
  private static final class Mem implements TokenStore {
    private final java.util.Map<String, String> m = new java.util.HashMap<>();
    @Override public void put(final String accountId, final String token) { m.put(accountId, token); }
    @Override public Optional<String> get(final String accountId) { return Optional.ofNullable(m.get(accountId)); }
    @Override public List<String> listAccounts() { return new ArrayList<>(m.keySet()); }
  }

  private Mem store;
  private SetTokenTool setTool;
  private GetTokenTool getTool;
  private ListTokensTool listTool;

  @BeforeEach
  void setup() {
    store = new Mem();
    setTool = new SetTokenTool(store);
    getTool = new GetTokenTool(store);
    listTool = new ListTokensTool(store);
  }

  @Test
  void setThenGet_reportsPresent() {
    setTool.call(Map.of("accountId", "acctA", "token", "tok-1"));
    var out = getTool.call(Map.of("accountId", "acctA"));
    String text = (String) out.get(0).get("text");
    assertTrue(text.contains("true"));
  }

  @Test
  void listTokens_showsAccountsOrNone() {
    // none
    String t0 = (String) listTool.call(Map.of()).get(0).get("text");
    assertTrue(t0.contains("none"));
    // add one
    setTool.call(Map.of("accountId", "acctB", "token", "tok-2"));
    String t1 = (String) listTool.call(Map.of()).get(0).get("text");
    assertTrue(t1.contains("acctB"));
  }

  @Test
  void missingArgs_returnHelpfulError() {
    String t = (String) getTool.call(Map.of()).get(0).get("text");
    assertTrue(t.contains("accountId"));
  }
}
