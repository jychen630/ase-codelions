package com.team.mcp.audit;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Audit row for a single MCP {@code tools/call} invocation.
 */
@Entity
@Table(
    name = "tool_call_audit",
    indexes = {
        @Index(name = "ix_audit_time", columnList = "created_at")
    }
)
public final class ToolCallAudit {

  /**
   * Maximum length for the JSON-RPC method column (e.g., {@code tools/call}).
   */
  public static final int LEN_METHOD = 40;

  /**
   * Maximum length for the tool name column (e.g., {@code search_tweets}).
   */
  public static final int LEN_TOOL = 64;

  /**
   * Maximum length for the logical account id column.
   */
  public static final int LEN_ACCOUNT = 64;

  /**
   * Maximum length for the stored error message column.
   */
  public static final int LEN_ERROR = 255;

  /** DB id. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** JSON-RPC method (e.g., {@code tools/call}). */
  @Column(name = "rpc_method", length = LEN_METHOD, nullable = false)
  private String rpcMethod;

  /** Tool name when method is {@code tools/call}. */
  @Column(name = "tool_name", length = LEN_TOOL)
  private String toolName;

  /** Optional logical account id. */
  @Column(name = "account_id", length = LEN_ACCOUNT)
  private String accountId;

  /** True on success. */
  @Column(name = "ok", nullable = false)
  private boolean ok;

  /** Duration in milliseconds. */
  @Column(name = "duration_ms", nullable = false)
  private long durationMs;

  /** Optional error code. */
  @Column(name = "error_code")
  private Integer errorCode;

  /** Optional error message (trimmed). */
  @Column(name = "error_message", length = LEN_ERROR)
  private String errorMessage;

  /** Creation timestamp. */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  /** JPA constructor. */
  protected ToolCallAudit() { }

  /**
   * Convenience constructor.
   *
   * @param method RPC method
   * @param tool tool name
   * @param acc account id
   * @param okFlag success flag
   * @param ms duration in ms
   * @param code error code (nullable)
   * @param msg error message (nullable)
   */
  public ToolCallAudit(final String method, final String tool,
      final String acc, final boolean okFlag, final long ms,
      final Integer code, final String msg) {
    this.rpcMethod = method;
    this.toolName = tool;
    this.accountId = acc;
    this.ok = okFlag;
    this.durationMs = ms;
    this.errorCode = code;
    this.errorMessage = msg;
  }

  /** @return the database identifier for this audit row */
  public Long getId() {
    return id;
  }
}
