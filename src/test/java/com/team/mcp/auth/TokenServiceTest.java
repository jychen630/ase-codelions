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
}
