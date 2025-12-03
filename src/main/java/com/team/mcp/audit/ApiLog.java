package com.team.mcp.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * API log entry persisted into the {@code api_logs} table.
 *
 * <p>Backed by Flyway migration {@code V1__init.sql}.</p>
 */
@Entity
@Table(name = "api_logs")
public final class ApiLog {

  /** Maximum tool name length (column length). */
  private static final int MAX_TOOL_LEN = 64;

  /** Maximum account id length (column length). */
  private static final int MAX_ACCOUNT_LEN = 128;

  /** Maximum params hash length (column length). */
  private static final int MAX_PARAMS_HASH_LEN = 128;

  /** Surrogate primary key. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Logical tool name or endpoint key (e.g., "get_home_timeline"). */
  @Column(name = "tool", length = MAX_TOOL_LEN, nullable = false)
  private String tool;

  /** Logical account id if known (may be null). */
  @Column(name = "account_id", length = MAX_ACCOUNT_LEN)
  private String accountId;

  /** Hash of request params/body to avoid storing raw payload. */
  @Column(name = "params_hash", length = MAX_PARAMS_HASH_LEN)
  private String paramsHash;

  /** HTTP status code returned to the client. */
  @Column(name = "status_code", nullable = false)
  private int statusCode;

  /** Optional error message when the call failed. */
  @Column(name = "error_msg")
  private String errorMsg;

  /** Creation timestamp managed by the database default. */
  @Column(
      name = "created_at",
      nullable = false,
      updatable = false,
      insertable = false
  )
  private Instant createdAt;

  /** JPA constructor. */
  protected ApiLog() {
    // for JPA
  }

  /**
   * Convenience constructor for the logging filter.
   *
   * @param toolValue logical tool/endpoint name
   * @param accountIdValue caller account id (nullable)
   * @param paramsHashValue params hash string
   * @param statusCodeValue HTTP status code
   * @param errorMsgValue optional error message
   */
  public ApiLog(
      final String toolValue,
      final String accountIdValue,
      final String paramsHashValue,
      final int statusCodeValue,
      final String errorMsgValue) {
    this.tool = toolValue;
    this.accountId = accountIdValue;
    this.paramsHash = paramsHashValue;
    this.statusCode = statusCodeValue;
    this.errorMsg = errorMsgValue;
  }

  /* ---------- Getters (for tests / diagnostics) ---------- */

  /**
   * Returns the surrogate primary key.
   *
   * @return log row id
   */
  public Long getId() {
    return id;
  }

  /**
   * Returns the logical tool or endpoint name.
   *
   * @return tool name
   */
  public String getTool() {
    return tool;
  }

  /**
   * Returns the caller account id, if known.
   *
   * @return account id or {@code null}
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Returns the hash of the request parameters/body.
   *
   * @return params hash string
   */
  public String getParamsHash() {
    return paramsHash;
  }

  /**
   * Returns the HTTP status code returned to the client.
   *
   * @return HTTP status code
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Returns the error message, if the call failed.
   *
   * @return error message or {@code null}
   */
  public String getErrorMsg() {
    return errorMsg;
  }

  /**
   * Returns the creation timestamp of this log row.
   *
   * @return creation time
   */
  public Instant getCreatedAt() {
    return createdAt;
  }
}

