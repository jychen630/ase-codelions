package com.team.mcp.twitter;

import com.team.mcp.auth.TokenStore;
import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Mastodon-backed implementation of {@link TwitterClient}.
 *
 * <p>Uses an access token stored in {@link TokenStore} for each
 * logical account id. Calls Mastodon APIs:
 *
 * <ul>
 *   <li>GET {@code /api/v1/timelines/home}</li>
 *   <li>POST {@code /api/v1/statuses}</li>
 * </ul>
 */
public final class MastodonClient implements TwitterClient {

  /** Base instance URL, e.g. {@code https://mastodon.social}. */
  private final String baseUrl;

  /** Token persistence from the OAuth flow. */
  private final TokenStore tokenStore;

  /** HTTP client used to talk to Mastodon. */
  private final RestTemplate http;

  /**
   * Construct a Mastodon-backed {@link TwitterClient}.
   *
   * @param instanceBaseUrl base URL of the Mastodon instance
   * @param store token store used to look up access tokens
   */
  public MastodonClient(final String instanceBaseUrl, final TokenStore store) {
    this.baseUrl = normalize(instanceBaseUrl);
    this.tokenStore = Objects.requireNonNull(store, "tokenStore");
    this.http = new RestTemplate();
  }

  private static String normalize(final String base) {
    if (base == null || base.isBlank()) {
      throw new IllegalArgumentException("instance base URL required");
    }
    // Strip trailing slash to avoid double slashes.
    return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
  }

  /**
   * Look up the stored access token for a logical account id.
   *
   * @param accountId logical account id
   * @return non-blank access token
   * @throws TwitterException if no token is stored
   */
  private String requireToken(final String accountId) throws TwitterException {
    return tokenStore.get(accountId)
        .filter(t -> !t.isBlank())
        .orElseThrow(() -> new TwitterException(
            "No stored access token for accountId=" + accountId));
  }

  @Override
  public String postTweet(final String accountId, final String text)
      throws TwitterException {
    final String token = requireToken(accountId);

    final String url = baseUrl + "/api/v1/statuses";

    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setBearerAuth(token);

    final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("status", text);

    final HttpEntity<MultiValueMap<String, String>> req =
        new HttpEntity<>(form, headers);

    try {
      final ResponseEntity<Map<String, Object>> resp = http.exchange(
          url,
          HttpMethod.POST,
          req,
          new ParameterizedTypeReference<Map<String, Object>>() { }
      );

      if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
        throw new TwitterException(
            "Non-2xx from Mastodon POST /statuses: "
                + resp.getStatusCodeValue());
      }

      final Object idObj = resp.getBody().get("id");
      if (idObj == null) {
        throw new TwitterException("Mastodon POST /statuses did not return id");
      }
      return idObj.toString();
    } catch (RestClientException ex) {
      throw new TwitterException("Error posting status to Mastodon", ex);
    }
  }

  @Override
  public List<Tweet> getHomeTimeline(final String accountId, final int count)
      throws TwitterException {
    final String token = requireToken(accountId);

    final int limit = Math.max(1, Math.min(count, 40)); // Mastodon max ~40
    final String url = baseUrl + "/api/v1/timelines/home?limit=" + limit;

    final HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    final HttpEntity<Void> req = new HttpEntity<>(headers);

    try {
      final ResponseEntity<List<Map<String, Object>>> resp = http.exchange(
          url,
          HttpMethod.GET,
          req,
          new ParameterizedTypeReference<List<Map<String, Object>>>() { }
      );

      if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
        throw new TwitterException(
            "Non-2xx from Mastodon GET /timelines/home: "
                + resp.getStatusCodeValue());
      }

      final List<Map<String, Object>> body = resp.getBody();
      final List<Tweet> result = new ArrayList<>(body.size());

      for (Map<String, Object> status : body) {
        final String id = valueAsString(status.get("id"));
        final Map<String, Object> account =
            valueAsMap(status.get("account"));
        final String user = valueAsString(
            account == null ? null : account.get("acct"));
        final String contentHtml = valueAsString(status.get("content"));
        final String createdAtStr = valueAsString(status.get("created_at"));

        final String text = stripHtml(contentHtml);
        final Instant createdAt =
            createdAtStr.isEmpty()
                ? Instant.now()
                : Instant.parse(createdAtStr);

        result.add(new Tweet(id, user, text, createdAt));
      }

      return result;
    } catch (RestClientException ex) {
      throw new TwitterException("Error fetching timeline from Mastodon", ex);
    }
  }

  private static String valueAsString(final Object o) {
    return o == null ? "" : o.toString();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> valueAsMap(final Object o) {
    if (o instanceof Map<?, ?> m) {
      return (Map<String, Object>) m;
    }
    return null;
  }

  /**
   * Very small HTML stripper for Mastodon {@code content} field.
   * Not perfect, but good enough for this project.
   *
   * @param html raw HTML fragment returned by Mastodon
   * @return plain text with HTML tags removed (never {@code null})
   */
  private static String stripHtml(final String html) {
    if (html == null || html.isEmpty()) {
      return "";
    }
    return html.replaceAll("<[^>]+>", "");
  }
}

