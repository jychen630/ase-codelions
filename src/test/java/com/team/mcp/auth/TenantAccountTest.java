package com.team.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Simple constructor/getter test for TenantAccount.
 */
class TenantAccountTest {

  @Test
  void constructorAndGetters_work() {
    TenantAccount t = new TenantAccount("uid-1", "Alice");
    assertEquals("uid-1", t.getUid());
    assertEquals("Alice", t.getName());
    // Not persisted in unit test -> id is null
    assertNull(t.getId());
  }
}

