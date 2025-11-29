package com.team.mcp.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InMemoryQuotaServiceTest {

  @Test
  void defaultValues_areReasonable() {
    InMemoryQuotaService svc = new InMemoryQuotaService();
    assertTrue(svc.readsUsed() >= 0);
    assertEquals(100, svc.readsMax());
    assertTrue(svc.writesUsed() >= 0);
    assertEquals(500, svc.writesMax());
    assertEquals("monthly", svc.resetPeriodIso());
  }
}

