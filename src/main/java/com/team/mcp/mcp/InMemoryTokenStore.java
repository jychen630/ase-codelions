package com.team.mcp.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * In-memory {@link TokenStore} for local testing.
 * Only active when profile {@code "mem"} is enabled.
 */
@Profile("mem")
@Service
public final class InMemoryTokenStore implements TokenStore {

  /** In-memory map of logical account id to opaque token value. */
  private final ConcurrentHashMap<String, String> tokens =
      new ConcurrentHashMap<>();

  @Override
  public java.util.Optional<String> get(final String accountId) {
    if (accountId == null || accountId.isBlank()) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.ofNullable(tokens.get(accountId));
  }

  @Override
  public void put(final String accountId, final String token) {
    if (accountId == null || accountId.isBlank()) {
      throw new IllegalArgumentException("accountId required");
    }
    tokens.put(accountId, Objects.requireNonNull(token, "token"));
  }

  @Override
  public List<String> listAccounts() {
    return new ArrayList<>(tokens.keySet());
  }
}
