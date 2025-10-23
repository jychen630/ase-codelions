package com.team.mcp.security;

/** Supplies a logical account id for the current caller. */
public interface TokenProvider {
  /**
   * Returns the account id / tenant id for the current request.
   *
   * @return account id string
   */
  String accountIdForCaller();
}
