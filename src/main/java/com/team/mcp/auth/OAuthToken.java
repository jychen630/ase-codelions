package com.team.mcp.auth;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Minimal OAuth token record for a logical account.
 *
 * <p>Iteration-1: simple fields and timestamps. Replace or extend later
 * with real Twitter OAuth tokens and scopes.
 */
@Entity
@Table(name = "oauth_tokens")
public final class OAuthToken {

  /** Max length for account id column. */
  private static final int ACCOUNT_ID_MAX = 64;

  /** Max length for token columns. */
  private static final int TOKEN_MAX = 512;

  /** Surrogate primary key. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Logical account id (tenant). */
  @Column(name = "account_id", nullable = false, length = ACCOUNT_ID_MAX)
  private String accountId;

  /** Access token (base64 "encrypted" in Iteration-1). */
  @Column(name = "access_token", nullable = false, length = TOKEN_MAX)
  private String accessToken;

  /** Optional refresh token (base64 "encrypted"). */
  @Column(name = "refresh_token", length = TOKEN_MAX)
  private String refreshToken;

  /** Creation timestamp (UTC). */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Update timestamp (UTC). */
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** JPA requires a no-arg constructor. */
  protected OAuthToken() {
    // for JPA
  }

  /**
   * Convenience constructor.
   *
   * @param accountIdValue logical account id
   * @param accessTokenValue access token (already encoded if desired)
   * @param refreshTokenValue refresh token (already encoded if desired)
   */
  public OAuthToken(
      final String accountIdValue,
      final String accessTokenValue,
      final String refreshTokenValue) {
    this.accountId = accountIdValue;
    this.accessToken = accessTokenValue;
    this.refreshToken = refreshTokenValue;
  }

  /**
   * Set timestamps on insert.
   */
  @PrePersist
  void onCreate() {
    final Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * Update timestamp on update.
   */
  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
  }

  /**
   * Returns the primary key.
   *
   * @return id
   */
  public Long getId() {
    return id;
  }

  /**
   * Returns the logical account id.
   *
   * @return account id
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Sets the logical account id.
   *
   * @param accountIdValue account id
   */
  public void setAccountId(final String accountIdValue) {
    this.accountId = accountIdValue;
  }

  /**
   * Returns the access token (stored encoded).
   *
   * @return access token
   */
  public String getAccessToken() {
    return accessToken;
  }

  /**
   * Sets the access token (store encoded value).
   *
   * @param accessTokenValue token value
   */
  public void setAccessToken(final String accessTokenValue) {
    this.accessToken = accessTokenValue;
  }

  /**
   * Returns the refresh token (stored encoded) or null.
   *
   * @return refresh token or null
   */
  public String getRefreshToken() {
    return refreshToken;
  }

  /**
   * Sets the refresh token (store encoded value).
   *
   * @param refreshTokenValue token value
   */
  public void setRefreshToken(final String refreshTokenValue) {
    this.refreshToken = refreshTokenValue;
  }

  /**
   * Returns creation timestamp (UTC).
   *
   * @return created time
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Returns last update timestamp (UTC).
   *
   * @return updated time
   */
  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
