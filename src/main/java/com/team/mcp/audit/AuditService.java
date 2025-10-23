package com.team.mcp.audit;

import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Small facade to write audit rows to the database.
 */
@Service
public final class AuditService {

  /** Repository used to persist tool-call audit records. */
  private final ToolCallAuditRepository repo;

  /**
   * Ctor.
   *
   * @param repository audit repository
   */
  public AuditService(final ToolCallAuditRepository repository) {
    this.repo = Objects.requireNonNull(repository, "repository");
  }

  /**
   * Store one audit row.
   *
   * @param method JSON-RPC method
   * @param tool tool name (nullable)
   * @param account account id (nullable)
   * @param ok success flag
   * @param durationMs duration in ms
   * @param errCode error code (nullable)
   * @param errMsg error message (nullable)
   */
  public void save(final String method, final String tool,
      final String account, final boolean ok, final long durationMs,
      final Integer errCode, final String errMsg) {
    final ToolCallAudit row = new ToolCallAudit(
        method, tool, account, ok, durationMs, errCode, truncate(errMsg));
    repo.save(row);
  }

  /**
   * Truncate an error message to the maximum DB length.
   *
   * @param s original message (nullable)
   * @return truncated or original message; null if input null
   */
  private static String truncate(final String s) {
    if (s == null) {
      return null;
    }
    if (s.length() <= ToolCallAudit.LEN_ERROR) {
      return s;
    }
    return s.substring(0, ToolCallAudit.LEN_ERROR);
  }
}
