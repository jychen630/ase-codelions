package com.team.mcp.mcp;

/**
 * Quota information for check_quota_status tool.
 */
public interface QuotaService {

  /**
   * Reads used so far in the current period.
   *
   * @return used reads
   */
  int readsUsed();

  /**
   * Max reads allowed in the current period.
   *
   * @return max reads
   */
  int readsMax();

  /**
   * Writes used so far in the current period.
   *
   * @return used writes
   */
  int writesUsed();

  /**
   * Max writes allowed in the current period.
   *
   * @return max writes
   */
  int writesMax();

  /**
   * ISO-like label for reset period (e.g., monthly).
   *
   * @return label
   */
  String resetPeriodIso();
}
