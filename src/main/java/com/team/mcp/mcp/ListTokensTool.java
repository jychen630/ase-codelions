package com.team.mcp.mcp;

import com.team.mcp.auth.TokenStore;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * MCP tool to list accounts that have a stored token.
 *
 * <p>No input arguments.
 */
@Component
public final class ListTokensTool implements Tool {

  /** Token store dependency. */
  private final TokenStore store;

  /**
   * Creates the tool with a token store.
   *
   * @param tokenStore token store
   */
  public ListTokensTool(final TokenStore tokenStore) {
    this.store = tokenStore;
  }

  @Override
  public String name() {
    return "list_tokens";
  }

  @Override
  public String description() {
    return "List account ids that currently have tokens";
  }

  @Override
  public List<Map<String, Object>> call(final Map<String, Object> args) {
    final List<String> accounts = store.listAccounts();

    final String text = accounts.isEmpty()
        ? "accounts: none"
        : "accounts: " + accounts.stream()
            .sorted()
            .collect(Collectors.joining(", "));

    return List.of(Map.of("type", "text", "text", text));
  }
}
