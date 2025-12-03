package com.team.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for the placeholder crypto service.
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

  @Test
  void encrypt_nullReturnsNull() {
    SecretCryptoService svc = new SecretCryptoService("k");
    assertNull(svc.encrypt(null));
  }

  @Test
  void decrypt_nullReturnsNull() {
    SecretCryptoService svc = new SecretCryptoService("k");
    assertNull(svc.decrypt(null));
  }

  @Test
  void keyMaterial_returnsConfiguredValue() {
    SecretCryptoService svc = new SecretCryptoService("my-secret-key");
    assertEquals("my-secret-key", svc.keyMaterial());
  }
}

