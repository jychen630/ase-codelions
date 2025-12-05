package com.team.mcp.auth;

import java.util.List;
import java.util.Optional;

/**
 * Minimal token store abstraction used by MCP tools.
 *
 * <p>Iteration-1 keeps a single opaque token per logical account id.
 * Implementations may be in-memory or DB-backed.
 */
public interface TokenStore {

  /**
   * Look up the token for an account.
   *
   * @param accountId logical account id
   * @return token if present
   */
  Optional<String> get(String accountId);

  /**
   * Store or replace the token for an account.
   *
   * @param accountId logical account id
   * @param token opaque token value
   */
  void put(String accountId, String token);

  /**
   * List all account ids that currently have a token.
   *
   * @return list of account ids
   */
  List<String> listAccounts();
}
