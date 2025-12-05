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
  void upsertToken_blankAccountId_throwsException() {
    TokenService svc = new TokenService();
    
    assertThrows(IllegalArgumentException.class, () -> {
      svc.upsertToken("", "twitter", "token", null, null, null);
    });
    
    assertThrows(IllegalArgumentException.class, () -> {
      svc.upsertToken(null, "twitter", "token", null, null, null);
    });
  }

  @Test
  void getToken_shortAccessToken_returnsFullToken() {
    TokenService svc = new TokenService();
    svc.upsertToken("acctC", "twitter", "abc", null, null, null);
    
    TokenService.TokenView v = svc.getToken("acctC", "twitter");
    assertNotNull(v);
    assertEquals("abc", v.accessLast4()); // Full token when <= 4 chars
  }

  @Test
  void getToken_exactlyFourChars_returnsFullToken() {
    TokenService svc = new TokenService();
    svc.upsertToken("acctD", "twitter", "1234", null, null, null);
    
    TokenService.TokenView v = svc.getToken("acctD", "twitter");
    assertNotNull(v);
    assertEquals("1234", v.accessLast4());
  }

  @Test
  void getToken_nullAccessToken_returnsEmptyString() {
    TokenService svc = new TokenService();
    svc.upsertToken("acctE", "twitter", null, null, null, null);
    
    TokenService.TokenView v = svc.getToken("acctE", "twitter");
    assertNotNull(v);
    assertEquals("", v.accessLast4());
  }

  @Test
  void getToken_emptyAccessToken_returnsEmptyString() {
    TokenService svc = new TokenService();
    svc.upsertToken("acctF", "twitter", "", null, null, null);
    
    TokenService.TokenView v = svc.getToken("acctF", "twitter");
    assertNotNull(v);
    assertEquals("", v.accessLast4());
  }

  @Test
  void getToken_withRefreshToken_hasRefreshTrue() {
    TokenService svc = new TokenService();
    svc.upsertToken("acctG", "twitter", "token", "refresh", null, null);
    
    TokenService.TokenView v = svc.getToken("acctG", "twitter");
    assertNotNull(v);
    assertTrue(v.hasRefresh());
  }

  @Test
  void getToken_nullRefreshToken_hasRefreshFalse() {
    TokenService svc = new TokenService();
    svc.upsertToken("acctH", "twitter", "token", null, null, null);
    
    TokenService.TokenView v = svc.getToken("acctH", "twitter");
    assertNotNull(v);
    assertFalse(v.hasRefresh());
  }

  @Test
  void getToken_blankRefreshToken_hasRefreshFalse() {
    TokenService svc = new TokenService();
    svc.upsertToken("acctI", "twitter", "token", "   ", null, null);
    
    TokenService.TokenView v = svc.getToken("acctI", "twitter");
    assertNotNull(v);
    assertFalse(v.hasRefresh());
  }

  @Test
  void getToken_withExpiresAt_returnsIsoString() {
    TokenService svc = new TokenService();
    Instant exp = Instant.parse("2030-01-01T00:00:00Z");
    svc.upsertToken("acctJ", "twitter", "token", null, exp, null);
    
    TokenService.TokenView v = svc.getToken("acctJ", "twitter");
    assertNotNull(v);
    assertEquals("2030-01-01T00:00:00Z", v.expiresAtIso());
  }

  @Test
  void getToken_nullExpiresAt_returnsNull() {
    TokenService svc = new TokenService();
    svc.upsertToken("acctK", "twitter", "token", null, null, null);
    
    TokenService.TokenView v = svc.getToken("acctK", "twitter");
    assertNotNull(v);
    assertNull(v.expiresAtIso());
  }

  @Test
  void getToken_nonExistentAccount_returnsNull() {
    TokenService svc = new TokenService();
    
    TokenService.TokenView v = svc.getToken("nonexistent", "twitter");
    assertNull(v);
  }

  @Test
  void upsertToken_blankProvider_defaultsToTwitter() {
    TokenService svc = new TokenService();
    svc.upsertToken("acctL", "", "token", null, null, null);
    
    TokenService.TokenView v = svc.getToken("acctL", "twitter");
    assertNotNull(v);
    assertEquals("oken", v.accessLast4());
  }
}
