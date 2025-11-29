package com.team.mcp.audit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-side HTTP API for audit data (Iteration-2).
 *
 * <p>Endpoints:
 * <ul>
 *   <li><b>GET /audit/recent?limit=N</b> – last N tool calls,
 *       newest first</li>
 *   <li><b>GET /audit/summary?hours=H</b> – per-tool summary for
 *       last H hours</li>
 * </ul>
 */
@RestController
@RequestMapping(
    path = "/audit",
    produces = MediaType.APPLICATION_JSON_VALUE)
public final class AuditController {

  /** Service facade used to read audit data. */
  private final AuditService audit;

  /**
   * Construct the controller with the audit service.
   *
   * @param auditService audit service (read + write)
   */
  public AuditController(final AuditService auditService) {
    this.audit = auditService;
  }

  /**
   * Return the most recent tool-call audit rows.
   *
   * <p>Example: {@code GET /audit/recent?limit=20}
   *
   * @param limitParam requested max row count
   * @return newest-first list of {@link ToolCallAudit}
   */
  @GetMapping("/recent")
  public List<ToolCallAudit> recent(
      @RequestParam(name = "limit", defaultValue = "50")
      final int limitParam) {

    // Clamp to a safe range: 1..500
    final int limit = Math.max(1, Math.min(limitParam, 500));
    return audit.findRecent(limit);
  }

  /**
   * Per-tool summary for the last {@code hours} hours (default 24).
   *
   * <p>Example: {@code GET /audit/summary?hours=6}
   *
   * @param hoursParam look-back window in hours
   * @return list of per-tool summaries
   */
  @GetMapping("/summary")
  public List<AuditSummary> summary(
      @RequestParam(name = "hours", defaultValue = "24")
      final int hoursParam) {

    // Clamp to a reasonable range: 1..168 (one week)
    final int hours = Math.max(1, Math.min(hoursParam, 168));
    final Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
    return audit.summarizeByToolSince(since);
  }
}

