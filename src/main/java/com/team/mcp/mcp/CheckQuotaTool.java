package com.team.mcp.mcp;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Tool that returns a human-friendly quota status.
 */
@Component
public final class CheckQuotaTool implements Tool {

  /** Quota source. */
  private final QuotaService quota;

  /**
   * Creates the tool with a quota service.
   *
   * @param quotaService quota provider
   */
  public CheckQuotaTool(final QuotaService quotaService) {
    this.quota = quotaService;
  }

  @Override
  public String name() {
    return "check_quota_status";
  }

  @Override
  public String description() {
    return "Check current API quota usage";
  }

  @Override
  public List<Map<String, Object>> call(final Map<String, Object> args) {
    final String text = String.format(
        "Quota Status:%n- Reads: %d/%d used%n- Writes: %d/%d used%n"
            + "- Reset period: %s",
        quota.readsUsed(), quota.readsMax(),
        quota.writesUsed(), quota.writesMax(),
        quota.resetPeriodIso());

    return List.of(Map.of("type", "text", "text", text));
  }
}
