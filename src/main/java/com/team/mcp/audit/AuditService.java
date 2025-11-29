package com.team.mcp.audit;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Small facade to persist audit rows and provide read-side summaries.
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

  // ---------------------------------------------------------------------------
  // Iteration-2: read-side helpers
  // ---------------------------------------------------------------------------

  /**
   * Return the most recent N tool call audit rows, newest first.
   *
   * @param limit max number of rows to return
   * @return list of audit rows
   */
  public List<ToolCallAudit> findRecent(final int limit) {
    final int n = Math.max(1, Math.min(limit, 500)); // safety cap
    return repo
        .findAll(PageRequest.of(0, n,
            Sort.by(Sort.Direction.DESC, "createdAt")))
        .getContent();
  }

  /**
   * Summarize audit rows grouped by tool name since a given instant.
   *
   * @param sinceOnly include rows with createdAt >= sinceOnly
   * @return list of per-tool summaries
   */
  public List<AuditSummary> summarizeByToolSince(final Instant sinceOnly) {
    // Small project: just pull recent rows and aggregate in memory.
    final Instant cutoff = sinceOnly == null
        ? Instant.EPOCH
        : sinceOnly;

    final List<ToolCallAudit> rows = repo.findAll().stream()
        .filter(r -> r.getCreatedAt() != null
            && !r.getCreatedAt().isBefore(cutoff))
        .collect(Collectors.toList());

    final Map<String, List<ToolCallAudit>> byTool = rows.stream()
        .collect(Collectors.groupingBy(r ->
            r.getToolName() == null ? "(none)" : r.getToolName()));

    return byTool.entrySet().stream()
        .map(e -> summarizeTool(e.getKey(), e.getValue()))
        .sorted((a, b) -> Long.compare(b.totalCalls(), a.totalCalls()))
        .collect(Collectors.toList());
  }

  private static AuditSummary summarizeTool(
      final String toolName,
      final List<ToolCallAudit> rows) {

    long total = rows.size();
    long ok = rows.stream().filter(ToolCallAudit::isOk).count();
    long error = total - ok;
    long avgMs = total == 0
        ? 0L
        : Math.round(rows.stream()
            .mapToLong(ToolCallAudit::getDurationMs)
            .average()
            .orElse(0.0));

    Instant lastCall = rows.stream()
        .map(ToolCallAudit::getCreatedAt)
        .filter(Objects::nonNull)
        .max(Instant::compareTo)
        .orElse(null);

    return new AuditSummary(toolName, total, ok, error, avgMs, lastCall);
  }
}

