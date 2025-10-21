package com.team.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TwitterOAuthClientTest {

  @Test
  void buildAuthorizeUrl_includesResponseTypeCallbackAndState() {
    TwitterOAuthClient c = new TwitterOAuthClient();
    String url = c.buildAuthorizeUrl("state-1");

    assertTrue(url.contains("response_type=code"));
    assertTrue(url.contains("redirect_uri="));
    assertTrue(url.contains("state="));
    // includes our callback (URL-encoded)
    assertTrue(url.contains("localhost%3A8080%2Fauth%2Fcallback"));
  }

  @Test
  void exchangeCodeForAccessToken_isDeterministicAndPrefixed() {
    TwitterOAuthClient c = new TwitterOAuthClient();
    String a = c.exchangeCodeForAccessToken("codeX");
    String b = c.exchangeCodeForAccessToken("codeX");
    String c2 = c.exchangeCodeForAccessToken("codeY");

    assertNotNull(a);
    assertTrue(a.startsWith("tok_"));
    assertEquals(a, b);     // same code -> same token
    assertNotEquals(a, c2); // different code -> different token
  }
}
