package com.team.mcp.mcp;

import com.team.mcp.auth.TokenStore;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * MCP tool to store a token for an account.
 *
 * <p>Args:
 * <ul>
 *   <li>accountId: string</li>
 *   <li>token: string</li>
 * </ul>
 */
@Component
public final class SetTokenTool implements Tool {

  /** Token store dependency. */
  private final TokenStore store;

  /**
   * Creates the tool with a token store.
   *
   * @param tokenStore token store
   */
  public SetTokenTool(final TokenStore tokenStore) {
    this.store = tokenStore;
  }

  @Override
  public String name() {
    return "set_token";
  }

  @Override
  public String description() {
    return "Store a token for the given account id";
  }

  @Override
  public List<Map<String, Object>> call(final Map<String, Object> args) {
    final Object accObj = args.get("accountId");
    final Object tokObj = args.get("token");

    final boolean badAccount =
        !(accObj instanceof String) || ((String) accObj).isBlank();
    final boolean badToken =
        !(tokObj instanceof String) || ((String) tokObj).isBlank();

    if (badAccount || badToken) {
      return List.of(Map.of(
          "type", "text",
          "text", "error: 'accountId' and 'token' are required"));
    }

    final String accountId = (String) accObj;
    final String token = (String) tokObj;
    store.put(accountId, token);

    return List.of(Map.of("type", "text",
        "text", "OK: token stored for " + accountId));
  }
}
