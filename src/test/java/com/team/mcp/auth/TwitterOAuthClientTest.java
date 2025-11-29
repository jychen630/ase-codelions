package com.team.mcp.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Lightweight tests for TwitterOAuthClient (Mastodon OAuth adapter).
 *
 * These tests only verify how the authorize URL is built. We do NOT
 * call exchangeCodeForAccessToken here, because that method performs
 * a real HTTP request to the Mastodon instance.
 */
final class TwitterOAuthClientTest {

  @Test
  void authorizeUrlContainsExpectedBaseAndParams() {
    TwitterOAuthClient client = new TwitterOAuthClient();
    String state = "state-123";
    String url = client.buildAuthorizeUrl(state);

    // Basic structure
    assertTrue(url.startsWith("https://mastodon.social/oauth/authorize"));
    assertTrue(url.contains("response_type=code"));
    assertTrue(url.contains("client_id="));
    assertTrue(url.contains("redirect_uri="));
    assertTrue(url.contains("scope="));
    assertTrue(url.contains("state="));
  }

  @Test
  void authorizeUrlEncodesState() {
    TwitterOAuthClient client = new TwitterOAuthClient();
    String state = "a b+c";
    String url = client.buildAuthorizeUrl(state);

    // "a b+c" should be URL-encoded as "a+b%2Bc"
    assertTrue(url.contains("state=a+b%2Bc"));
  }
}
