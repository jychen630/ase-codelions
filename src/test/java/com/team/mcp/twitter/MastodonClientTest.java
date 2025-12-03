package com.team.mcp.twitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.team.mcp.auth.TokenStore;
import com.team.mcp.twitter.TwitterClient.TwitterException;
import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class MastodonClientTest {

  @Test
  void constructor_rejectsNullOrBlankBaseUrl() {
    TokenStore store = Mockito.mock(TokenStore.class);

    assertThrows(IllegalArgumentException.class, () ->
        new MastodonClient(null, store));

    assertThrows(IllegalArgumentException.class, () ->
        new MastodonClient("   ", store));
  }

  @Test
  void postTweet_successReturnsId() throws Exception {
    TokenStore store = Mockito.mock(TokenStore.class);
    Mockito.when(store.get("acct"))
        .thenReturn(Optional.of("token-123"));

    MastodonClient client =
        new MastodonClient("https://mastodon.example", store);

    RestTemplate rt =
        (RestTemplate) ReflectionTestUtils.getField(client, "http");
    MockRestServiceServer server =
        MockRestServiceServer.createServer(rt);

    String bodyJson = "{\"id\":\"42\",\"content\":\"ok\"}";

    server.expect(MockRestRequestMatchers.requestTo(
            "https://mastodon.example/api/v1/statuses"))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
        .andRespond(MockRestResponseCreators.withSuccess(
            bodyJson, MediaType.APPLICATION_JSON));

    String id = client.postTweet("acct", "hello world");
    assertEquals("42", id);

    server.verify();
  }

  @Test
  void postTweet_noTokenThrowsTwitterException() {
    TokenStore store = Mockito.mock(TokenStore.class);
    Mockito.when(store.get("acct"))
        .thenReturn(Optional.empty());

    MastodonClient client =
        new MastodonClient("https://mastodon.example", store);

    assertThrows(TwitterException.class, () ->
        client.postTweet("acct", "hello"));
  }

  @Test
  void postTweet_blankTokenThrowsTwitterException() {
    TokenStore store = Mockito.mock(TokenStore.class);
    Mockito.when(store.get("acct"))
        .thenReturn(Optional.of("   "));

    MastodonClient client =
        new MastodonClient("https://mastodon.example", store);

    assertThrows(TwitterException.class, () ->
        client.postTweet("acct", "hello"));
  }

  @Test
  void postTweet_non2xxResponseThrowsTwitterException() {
    TokenStore store = Mockito.mock(TokenStore.class);
    Mockito.when(store.get("acct"))
        .thenReturn(Optional.of("token-123"));

    MastodonClient client =
        new MastodonClient("https://mastodon.example", store);

    RestTemplate rt =
        (RestTemplate) ReflectionTestUtils.getField(client, "http");
    MockRestServiceServer server =
        MockRestServiceServer.createServer(rt);

    server.expect(MockRestRequestMatchers.requestTo(
            "https://mastodon.example/api/v1/statuses"))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
        .andRespond(MockRestResponseCreators.withServerError());

    assertThrows(TwitterException.class, () ->
        client.postTweet("acct", "hello"));

    server.verify();
  }

  @Test
  void postTweet_missingIdInBodyThrowsTwitterException() {
    TokenStore store = Mockito.mock(TokenStore.class);
    Mockito.when(store.get("acct"))
        .thenReturn(Optional.of("token-123"));

    MastodonClient client =
        new MastodonClient("https://mastodon.example", store);

    RestTemplate rt =
        (RestTemplate) ReflectionTestUtils.getField(client, "http");
    MockRestServiceServer server =
        MockRestServiceServer.createServer(rt);

    String bodyJson = "{\"content\":\"no id here\"}";

    server.expect(MockRestRequestMatchers.requestTo(
            "https://mastodon.example/api/v1/statuses"))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
        .andRespond(MockRestResponseCreators.withSuccess(
            bodyJson, MediaType.APPLICATION_JSON));

    assertThrows(TwitterException.class, () ->
        client.postTweet("acct", "hello"));

    server.verify();
  }

  @Test
  void postTweet_restClientExceptionWrappedInTwitterException() {
    TokenStore store = Mockito.mock(TokenStore.class);
    Mockito.when(store.get("acct"))
        .thenReturn(Optional.of("token-123"));

    MastodonClient client =
        new MastodonClient("https://mastodon.example", store);

    RestTemplate rt = Mockito.mock(RestTemplate.class);
    ReflectionTestUtils.setField(client, "http", rt);

    Mockito.when(rt.exchange(
        Mockito.anyString(),
        Mockito.any(HttpMethod.class),
        Mockito.any(),
        Mockito.any(org.springframework.core.ParameterizedTypeReference
            .class)
    )).thenThrow(new RestClientException("boom"));

    assertThrows(TwitterException.class, () ->
        client.postTweet("acct", "hello"));
  }

  @Test
  void getHomeTimeline_successMapsFieldsAndStripsHtml()
      throws Exception {
    TokenStore store = Mockito.mock(TokenStore.class);
    Mockito.when(store.get("acct"))
        .thenReturn(Optional.of("token-123"));

    MastodonClient client =
        new MastodonClient("https://mastodon.example", store);

    RestTemplate rt =
        (RestTemplate) ReflectionTestUtils.getField(client, "http");
    MockRestServiceServer server =
        MockRestServiceServer.createServer(rt);

    String json = "["
        + "{"
        + "\"id\":\"1\","
        + "\"account\":{\"acct\":\"alice\"},"
        + "\"content\":\"<p>Hello <b>world</b></p>\","
        + "\"created_at\":\"2025-01-02T03:04:05Z\""
        + "},"
        + "{"
        + "\"id\":\"2\","
        + "\"account\":\"not-a-map\","
        + "\"content\":null,"
        + "\"created_at\":\"\""
        + "}"
        + "]";

    server.expect(MockRestRequestMatchers.requestTo(
            "https://mastodon.example/api/v1/timelines/home?limit=3"))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(MockRestResponseCreators.withSuccess(
            json, MediaType.APPLICATION_JSON));

    List<Tweet> tweets = client.getHomeTimeline("acct", 3);
    assertEquals(2, tweets.size());

    Tweet t1 = tweets.get(0);
    assertEquals("1", t1.id());
    assertEquals("alice", t1.user());
    assertEquals("Hello world", t1.text());
    assertEquals(Instant.parse("2025-01-02T03:04:05Z"),
        t1.createdAt());

    Tweet t2 = tweets.get(1);
    assertEquals("2", t2.id());
    assertEquals("", t2.user());
    assertEquals("", t2.text());
    assertNotNull(t2.createdAt(),
        "empty created_at should default to now()");

    server.verify();
  }

  @Test
  void getHomeTimeline_clampsLimitBetweenOneAndForty()
      throws Exception {
    TokenStore store = Mockito.mock(TokenStore.class);
    Mockito.when(store.get("acct"))
        .thenReturn(Optional.of("token-123"));

    MastodonClient client =
        new MastodonClient("https://mastodon.example", store);

    RestTemplate rt =
        (RestTemplate) ReflectionTestUtils.getField(client, "http");

    MockRestServiceServer server1 =
        MockRestServiceServer.createServer(rt);
    server1.expect(MockRestRequestMatchers.requestTo(
            "https://mastodon.example"
                + "/api/v1/timelines/home?limit=1"))
        .andRespond(MockRestResponseCreators.withSuccess(
            "[]", MediaType.APPLICATION_JSON));
    client.getHomeTimeline("acct", 0);
    server1.verify();

    MockRestServiceServer server2 =
        MockRestServiceServer.createServer(rt);
    server2.expect(MockRestRequestMatchers.requestTo(
            "https://mastodon.example"
                + "/api/v1/timelines/home?limit=40"))
        .andRespond(MockRestResponseCreators.withSuccess(
            "[]", MediaType.APPLICATION_JSON));
    client.getHomeTimeline("acct", 100);
    server2.verify();
  }

  @Test
  void getHomeTimeline_non2xxThrowsTwitterException() {
    TokenStore store = Mockito.mock(TokenStore.class);
    Mockito.when(store.get("acct"))
        .thenReturn(Optional.of("token-123"));

    MastodonClient client =
        new MastodonClient("https://mastodon.example", store);

    RestTemplate rt =
        (RestTemplate) ReflectionTestUtils.getField(client, "http");
    MockRestServiceServer server =
        MockRestServiceServer.createServer(rt);

    server.expect(MockRestRequestMatchers.requestTo(
            "https://mastodon.example"
                + "/api/v1/timelines/home?limit=2"))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(MockRestResponseCreators.withServerError());

    assertThrows(TwitterException.class, () ->
        client.getHomeTimeline("acct", 2));

    server.verify();
  }

  @Test
  void getHomeTimeline_restClientExceptionWrappedInTwitterException() {
    TokenStore store = Mockito.mock(TokenStore.class);
    Mockito.when(store.get("acct"))
        .thenReturn(Optional.of("token-123"));

    MastodonClient client =
        new MastodonClient("https://mastodon.example", store);

    RestTemplate rt = Mockito.mock(RestTemplate.class);
    ReflectionTestUtils.setField(client, "http", rt);

    Mockito.when(rt.exchange(
        Mockito.anyString(),
        Mockito.any(HttpMethod.class),
        Mockito.any(),
        Mockito.any(org.springframework.core.ParameterizedTypeReference
            .class)
    )).thenThrow(new RestClientException("boom"));

    assertThrows(TwitterException.class, () ->
        client.getHomeTimeline("acct", 5));
  }
}

