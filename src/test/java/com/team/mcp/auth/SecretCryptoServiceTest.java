package com.team.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Round-trip tests for the placeholder crypto service.
 */
class SecretCryptoServiceTest {

  @Test
  void encryptDecrypt_roundTrip() {
    SecretCryptoService svc = new SecretCryptoService("test-key");
    String enc = svc.encrypt("hello");
    assertNotEquals("hello", enc);
    String dec = svc.decrypt(enc);
    assertEquals("hello", dec);
  }
}
