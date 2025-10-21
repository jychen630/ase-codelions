package com.team.mcp.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Token record stored per logical account id.
 *
 * <p>Iteration-1 keeps it minimal: one opaque token string keyed by
 * {@code accountId}. Swap out in Iteration-2 for real OAuth secrets.
 */
@Entity
@Table(name = "token_credentials")
public final class TokenCredential {

  /** Max length for account id column. */
  private static final int ACCOUNT_ID_LEN = 128;

  /** Max length for token column. */
  private static final int TOKEN_LEN = 1024;

  /** Stable logical account id (e.g., "acctA"). */
  @Id
  @Column(
      name = "account_id",
      length = ACCOUNT_ID_LEN,
      nullable = false,
      updatable = false)
  private String accountId;

  /** Opaque token material (plaintext/base64 in Iteration-1). */
  @Column(name = "token", length = TOKEN_LEN, nullable = false)
  private String token;

  /** JPA constructor. */
  protected TokenCredential() {
    // for JPA
  }

  /**
   * Creates a token credential.
   *
   * @param accountIdParam id to store
   * @param tokenParam token value
   */
  public TokenCredential(
      final String accountIdParam,
      final String tokenParam) {
    this.accountId = accountIdParam;
    this.token = tokenParam;
  }

  /** @return account id */
  public String getAccountId() {
    return accountId;
  }

  /** @return token string */
  public String getToken() {
    return token;
  }

  /**
   * Update token value.
   *
   * @param tokenParam new token
   */
  public void setToken(final String tokenParam) {
    this.token = tokenParam;
  }
}
