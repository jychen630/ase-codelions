package com.team.mcp.mcp;

import java.util.Set;

/**
 * Simple token storage abstraction used by MCP auth tools.
 *
 * <p>This Iteration-1 implementation is in memory. Later, replace with a
 * persistent store (e.g., JPA) without changing tool code.
 */
public interface TokenStore {

  /**
   * Save or replace a token for the given account.
   *
   * @param accountId logical account identifier
   * @param token opaque credential value
   */
  void putToken(String accountId, String token);

  /**
   * Retrieve the token for an account.
   *
   * @param accountId logical account identifier
   * @return token string or {@code null} if none found
   */
  String getToken(String accountId);

  /**
   * List all account ids that currently have a stored token.
   *
   * @return a set of account ids (never null)
   */
  Set<String> listAccounts();
}
