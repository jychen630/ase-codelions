package com.team.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

  @Test
  void upsert_and_getToken_redactsAccess_keepsFlagsAndMetadata() {
    TokenService svc = new TokenService();

    Instant exp = Instant.parse("2030-01-01T00:00:00Z");
    svc.upsertToken(
        "acctA",
        "twitter",
        "abcdef123456",
        "refresh-xyz",
        exp,
        "tweet.read,tweet.write"
    );

    TokenService.TokenView v = svc.getToken("acctA", "twitter");
    assertNotNull(v);
    assertEquals("3456", v.accessLast4());          // last 4 chars
    assertTrue(v.hasRefresh());
    assertEquals("2030-01-01T00:00:00Z", v.expiresAtIso());
    assertEquals("tweet.read,tweet.write", v.scopesCsv());
  }

  @Test
  void provider_defaultsToTwitter_whenBlankOrNull() {
    TokenService svc = new TokenService();
    svc.upsertToken("acctB", null, "abc", null, null, null);

    // ask with blank provider -> should still find it
    TokenService.TokenView v1 = svc.getToken("acctB", "");
    assertNotNull(v1);
    assertEquals("abc", v1.accessLast4()); // short token is not truncated

    // ask with explicit "twitter" -> same entry
    TokenService.TokenView v2 = svc.getToken("acctB", "twitter");
    assertNotNull(v2);
    assertEquals("abc", v2.accessLast4());
  }

  @Test
  void getToken_returnsNullWhenNoEntry() {
    TokenService svc = new TokenService();
    assertNull(svc.getToken("missing", "twitter"));
  }

  @Test
  void clearToken_removesEntry() {
    TokenService svc = new TokenService();
    svc.upsertToken("acctC", "twitter", "abcd", null, null, null);
    assertNotNull(svc.getToken("acctC", "twitter"));

    svc.clearToken("acctC", "twitter");
    assertNull(svc.getToken("acctC", "twitter"));
  }

  @Test
  void upsertToken_requiresNonBlankAccountId() {
    TokenService svc = new TokenService();
    assertThrows(IllegalArgumentException.class,
        () -> svc.upsertToken("  ", "twitter", "x", null, null, null));
  }

  @Test
  void getToken_handlesNullOrEmptyAccess_andNoRefreshOrExpiry() {
    TokenService svc = new TokenService();

    // null access and refresh, null expiry
    svc.upsertToken("acctD", "twitter", null, null, null, "scope1");

    TokenService.TokenView v = svc.getToken("acctD", "twitter");
    assertNotNull(v);
    assertEquals("", v.accessLast4());
    assertFalse(v.hasRefresh());
    assertNull(v.expiresAtIso());
    assertEquals("scope1", v.scopesCsv());
  }

  @Test
  void getToken_shortAccessNotTruncated() {
    TokenService svc = new TokenService();
    svc.upsertToken("acctE", "twitter", "xyz", "", null, null);

    TokenService.TokenView v = svc.getToken("acctE", "twitter");
    assertEquals("xyz", v.accessLast4());
  }

  @Test
  void clearToken_usesDefaultProviderWhenBlank() {
    TokenService svc = new TokenService();

    // Store using default provider logic (null -> "twitter")
    svc.upsertToken("acctF", null, "xyz123", null, null, null);
    assertNotNull(svc.getToken("acctF", "twitter"));

    // Clear with blank provider -> should map to same key and remove it
    svc.clearToken("acctF", "");
    assertNull(svc.getToken("acctF", "twitter"));
  }

}

