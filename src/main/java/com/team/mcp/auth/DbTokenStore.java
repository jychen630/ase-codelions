package com.team.mcp.auth;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database-backed implementation of the token store used by MCP tools.
 *
 * <p>Marked {@link Primary} so it overrides the in-memory store when both
 * are on the classpath. No profile flag needed for Iteration-1.
 *
 * <p>Iteration-1 security note: tokens are persisted using
 * {@link SecretCryptoService} (base64 placeholder). Replace with a real
 * AEAD cipher/KMS in Iteration-2 without changing this interface.
 */
@Primary
@Service
@SuppressWarnings("checkstyle:DesignForExtension")
public class DbTokenStore implements TokenStore {

  /** Repository for persistence. */
  private final TokenCredentialRepository repo;

  /** Simple crypto adapter (base64 in Iteration-1). */
  private final SecretCryptoService crypto;

  /**
   * Creates the store with a repository and crypto service.
   *
   * @param repoParam JPA repository
   * @param cryptoParam crypto adapter used to protect secrets at rest
   */
  public DbTokenStore(
      final TokenCredentialRepository repoParam,
      final SecretCryptoService cryptoParam) {
    this.repo = Objects.requireNonNull(repoParam, "repo");
    this.crypto = Objects.requireNonNull(cryptoParam, "crypto");
  }

  /**
   * Retrieve a plaintext token for the given account id, if present.
   *
   * @param accountId logical account id
   * @return optional plaintext token
   */
  @Override
  @Transactional(readOnly = true)
  public Optional<String> get(final String accountId) {
    return repo.findByAccountId(accountId)
        .map(TokenCredential::getToken)
        .map(crypto::decrypt);
  }

  /**
   * Store or update the token for an account id.
   *
   * @param accountId logical account id
   * @param token plaintext token to persist (will be encoded)
   */
  @Override
  @Transactional
  public void put(final String accountId, final String token) {
    final String enc = crypto.encrypt(token);
    final TokenCredential cred =
        repo.findByAccountId(accountId)
            .map(existing -> {
              existing.setToken(enc);
              return existing;
            })
            .orElseGet(() -> new TokenCredential(accountId, enc));
    repo.save(cred);
  }

  /**
   * List account ids that currently have a persisted token.
   *
   * @return list of logical account ids
   */
  @Override
  @Transactional(readOnly = true)
  public List<String> listAccounts() {
    return repo.listAccountIds();
  }
}
