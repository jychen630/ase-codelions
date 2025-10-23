package com.team.mcp.mcp;

import org.springframework.stereotype.Service;

/**
 * Simple in-memory quota service for Iteration-1.
 *
 * <p>Provides fixed demo values so the MCP tools can report a quota
 * status without requiring a DB. Replace later with a persistent
 * implementation.
 */
@Service
public final class InMemoryQuotaService implements QuotaService {

  /** Demo limit for reads. */
  private static final int READ_MAX = 100;
  /** Demo limit for writes. */
  private static final int WRITE_MAX = 500;

  /** Initial demo value for reads used (avoid magic number). */
  private static final int INIT_READS = 5;
  /** Initial demo value for writes used (avoid magic number). */
  private static final int INIT_WRITES = 12;

  /** Demo used reads. */
  private int reads = INIT_READS;
  /** Demo used writes. */
  private int writes = INIT_WRITES;

  @Override
  public int readsUsed() {
    return reads;
  }

  @Override
  public int readsMax() {
    return READ_MAX;
  }

  @Override
  public int writesUsed() {
    return writes;
  }

  @Override
  public int writesMax() {
    return WRITE_MAX;
  }

  @Override
  public String resetPeriodIso() {
    return "monthly";
  }
}
