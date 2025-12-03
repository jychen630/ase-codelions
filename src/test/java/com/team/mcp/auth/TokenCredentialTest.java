package com.team.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Small test for TokenCredential DTO.
 */
class TokenCredentialTest {

  @Test
  void constructorGettersAndSetter_work() {
    TokenCredential c = new TokenCredential("acctA", "tok1");
    assertEquals("acctA", c.getAccountId());
    assertEquals("tok1", c.getToken());

    c.setToken("tok2");
    assertEquals("tok2", c.getToken());
  }
}

