package com.team.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Exercises OAuthToken's constructor, getters/setters, and JPA callbacks.
 */
class OAuthTokenTest {

  @Test
  void constructorGettersSettersAndLifecycleCallbacks_work() throws Exception {
    OAuthToken tok = new OAuthToken("acct", "accTok", "refTok");
    assertEquals("acct", tok.getAccountId());
    assertEquals("accTok", tok.getAccessToken());
    assertEquals("refTok", tok.getRefreshToken());

    tok.setAccountId("acct2");
    tok.setAccessToken("accTok2");
    tok.setRefreshToken("refTok2");

    assertEquals("acct2", tok.getAccountId());
    assertEquals("accTok2", tok.getAccessToken());
    assertEquals("refTok2", tok.getRefreshToken());

    // Call @PrePersist / @PreUpdate methods via reflection
    Method onCreate = OAuthToken.class.getDeclaredMethod("onCreate");
    onCreate.setAccessible(true);
    onCreate.invoke(tok);

    Instant created = tok.getCreatedAt();
    Instant firstUpdated = tok.getUpdatedAt();
    assertNotNull(created);
    assertNotNull(firstUpdated);

    Method onUpdate = OAuthToken.class.getDeclaredMethod("onUpdate");
    onUpdate.setAccessible(true);
    onUpdate.invoke(tok);

    Instant secondUpdated = tok.getUpdatedAt();
    assertNotNull(secondUpdated);
    // updatedAt should be >= createdAt
    assertFalse(secondUpdated.isBefore(created));
  }
}

