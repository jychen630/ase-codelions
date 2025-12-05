package com.team.mcp.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Iteration-1 placeholder "crypto" that base64-encodes strings.
 *
 * <p><strong>Not secure.</strong> Replace with AES-GCM or a KMS-backed
 * implementation in the next iteration.
 */
@Service
public class SecretCryptoService {

  /** Placeholder key so we can wire config; not used by base64. */
  private final String keyMaterial;

  /**
   * Creates the service with key material from configuration.
   *
   * @param cfgKey value of configuration property {@code app.kms.key}
   */
  public SecretCryptoService(
      @Value("${app.kms.key:change-me-in-env}") final String cfgKey) {
    this.keyMaterial = cfgKey;
  }

  /**
   * "Encrypt" by base64-encoding the input.
   *
   * @param plaintext plain text to encode; if {@code null} returns {@code null}
   * @return base64 representation of {@code plaintext}, or {@code null}
   */
  public String encrypt(final String plaintext) {
    if (plaintext == null) {
      return null;
    }
    final byte[] bytes = plaintext.getBytes(StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(bytes);
  }

  /**
   * "Decrypt" by base64-decoding the input.
   *
   * @param ciphertext base64 text to decode; if {@code null} returns
   *     {@code null}
   * @return decoded UTF-8 string, or {@code null}
   */
  public String decrypt(final String ciphertext) {
    if (ciphertext == null) {
      return null;
    }
    final byte[] bytes = Base64.getDecoder().decode(ciphertext);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  /**
   * Exposes the configured key material for diagnostics.
   *
   * @return the raw configuration value of {@code app.kms.key}
   */
  public String keyMaterial() {
    return keyMaterial;
  }
}
