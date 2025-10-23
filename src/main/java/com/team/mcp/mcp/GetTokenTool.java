package com.team.mcp.mcp;

import com.team.mcp.auth.TokenStore;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * MCP tool to report whether a token exists for an account.
 *
 * <p>Args:
 * <ul>
 *   <li>accountId: string</li>
 * </ul>
 */
@Component
public final class GetTokenTool implements Tool {

  /** Token store dependency. */
  private final TokenStore store;

  /**
   * Creates the tool with a token store.
   *
   * @param tokenStore token store
   */
  public GetTokenTool(final TokenStore tokenStore) {
    this.store = tokenStore;
  }

  @Override
  public String name() {
    return "get_token";
  }

  @Override
  public String description() {
    return "Report whether a token exists for an account";
  }

  @Override
  public List<Map<String, Object>> call(final Map<String, Object> args) {
    final Object accObj = args.get("accountId");
    if (!(accObj instanceof String) || ((String) accObj).isBlank()) {
      return List.of(Map.of(
          "type", "text",
          "text", "error: 'accountId' is required"));
    }

    final String accountId = (String) accObj;
    final boolean present = store.get(accountId).isPresent();

    return List.of(Map.of("type", "text", "text",
        "token present: " + present));
  }
}
