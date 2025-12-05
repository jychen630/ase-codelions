package com.team.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for TwitterOAuthClient.exchangeWithMetadata using a mocked HttpClient.
 */
class TwitterOAuthClientExchangeTest {

  private TwitterOAuthClient client;
  private HttpClient http;

  // Use raw type to avoid generic clashes with HttpClient.send(...)
  @SuppressWarnings("rawtypes")
  private HttpResponse response;

  @BeforeEach
  void setUp() throws Exception {
    client = new TwitterOAuthClient();
    http = mock(HttpClient.class);
    // raw mock, fine for our usage in tests
    response = mock(HttpResponse.class);

    // Replace the private final HttpClient field with our mock
    Field f = TwitterOAuthClient.class.getDeclaredField("http");
    f.setAccessible(true);
    f.set(client, http);
  }

  @Test
  @SuppressWarnings("unchecked")
  void exchangeWithMetadata_successParsesAccessToken() throws Exception {
    when(http.send(any(), any())).thenReturn(response);
    when(response.statusCode()).thenReturn(200);
    when(response.body())
        .thenReturn("{\"access_token\":\"tok123\",\"scope\":\"ignored\"}");

    TwitterOAuthClient.OAuthTokens tokens =
        client.exchangeWithMetadata("code-1");

    assertEquals("tok123", tokens.accessToken());
    assertNull(tokens.refreshToken());
    assertNotNull(tokens.expiresIn());
    assertTrue(tokens.expiresIn() > 0);
    assertEquals(
        "read:statuses write:statuses read:accounts",
        tokens.scope()
    );
  }

  @Test
  @SuppressWarnings("unchecked")
  void exchangeWithMetadata_non2xxStatusWrapsIllegalStateInRuntimeException()
      throws Exception {
    when(http.send(any(), any())).thenReturn(response);
    when(response.statusCode()).thenReturn(500);
    when(response.body()).thenReturn("server-error");

    RuntimeException ex = assertThrows(
        RuntimeException.class,
        () -> client.exchangeWithMetadata("code-err")
    );
    assertTrue(ex.getCause() instanceof IllegalStateException);
  }

  @Test
  @SuppressWarnings("unchecked")
  void exchangeWithMetadata_missingAccessTokenWrapsIllegalStateInRuntimeException()
      throws Exception {
    when(http.send(any(), any())).thenReturn(response);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("{\"no_access\":true}");

    RuntimeException ex = assertThrows(
        RuntimeException.class,
        () -> client.exchangeWithMetadata("code-missing")
    );
    assertTrue(ex.getCause() instanceof IllegalStateException);
  }

  @Test
  @SuppressWarnings("unchecked")
  void exchangeWithMetadata_httpClientExceptionWrappedInRuntimeException()
      throws Exception {
    when(http.send(any(), any())).thenThrow(new IOException("boom"));

    RuntimeException ex = assertThrows(
        RuntimeException.class,
        () -> client.exchangeWithMetadata("code-io")
    );
    assertTrue(ex.getCause() instanceof IOException);
  }
}

