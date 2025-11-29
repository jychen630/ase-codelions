package com.team.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TokenCredentialEntityTest {

  @Test
  void constructor_getters_and_setter() {
    TokenCredential tc = new TokenCredential("acctA", "token1");
    assertEquals("acctA", tc.getAccountId());
    assertEquals("token1", tc.getToken());

    tc.setToken("token2");
    assertEquals("token2", tc.getToken());
  }
}

