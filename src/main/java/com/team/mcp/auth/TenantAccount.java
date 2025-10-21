package com.team.mcp.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Tenant account who owns tokens. Could be a user or a workspace.
 */
@Entity
@Table(
    name = "tenant_account",
    indexes = {
      @Index(
          name = "ux_tenant_uid",
          columnList = "uid",
          unique = true
      )
    }
)
public final class TenantAccount {

  /** Max length for UID column. */
  private static final int UID_MAX = 128;

  /** Max length for display name column. */
  private static final int NAME_MAX = 128;

  /** DB id. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Stable external id (e.g., email or org id). */
  @Column(nullable = false, length = UID_MAX, unique = true)
  private String uid;

  /** Optional display name. */
  @Column(length = NAME_MAX)
  private String name;

  /** JPA requirement. */
  protected TenantAccount() {
    // for JPA
  }

  /**
   * Creates a tenant account.
   *
   * @param uidValue external id
   * @param nameValue display name
   */
  public TenantAccount(final String uidValue, final String nameValue) {
    this.uid = uidValue;
    this.name = nameValue;
  }

  /**
   * Returns the database identifier.
   *
   * @return surrogate primary key
   */
  public Long getId() {
    return id;
  }

  /**
   * Returns the stable external id.
   *
   * @return tenant uid (e.g., email or org id)
   */
  public String getUid() {
    return uid;
  }

  /**
   * Returns the display name, if present.
   *
   * @return human-readable name or {@code null}
   */
  public String getName() {
    return name;
  }
}
