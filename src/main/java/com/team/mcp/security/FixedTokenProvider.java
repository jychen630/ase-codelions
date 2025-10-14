package com.team.mcp.security;

import org.springframework.stereotype.Component;

/**
 * Temporary provider for Iteration-1; always returns a fixed account id.
 */
@Component
public final class FixedTokenProvider implements TokenProvider {

  /**
   * Returns a constant account id for the caller.
   *
   * @return the fixed logical account id
   */
  @Override
  public String accountIdForCaller() {
    return "test-account";
  }
}
