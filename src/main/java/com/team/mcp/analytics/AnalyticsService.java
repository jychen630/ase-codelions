package com.team.mcp.analytics;

import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.TwitterClient.TwitterException;
import com.team.mcp.twitter.dto.Tweet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Lightweight analytics over the fake Twitter timeline (or DB).
 *
 * <p>If {@code app.search.source=db} is configured, analytics will try
 * to read recent tweets from the {@code tweets} table via JDBC and
 * gracefully fall back to the timeline if unavailable.
 */
@Service
public class AnalyticsService {

  /** How many tweets to fetch for analytics. */
  private static final int DEFAULT_FETCH = 100;

  /** How many top hashtags to return by default. */
  private static final int TOP_N = 5;

  /** Client used to obtain tweets (fake for Iteration 1, real later). */
  private final TwitterClient twitter;

  /** Property switch: {@code "timeline"} (default) or {@code "db"}. */
  private final String source;

  /** Optional JDBC access; may be {@code null}. */
  private final JdbcTemplate jdbc;

  /**
   * Create the service.
   *
   * @param twitterClient client used to fetch tweets
   * @param jdbcTemplate optional jdbc template
   * @param searchSource {@code "timeline"} (default) or {@code "db"}
   */
  public AnalyticsService(
      final TwitterClient twitterClient,
      final JdbcTemplate jdbcTemplate,
      @Value("${app.search.source:timeline}") final String searchSource) {
    this.twitter = Objects.requireNonNull(twitterClient, "twitter");
    this.jdbc = jdbcTemplate;
    this.source = searchSource == null ? "timeline" : searchSource.trim();
  }

  /**
   * Return the top N hashtags for an account's home timeline.
   *
   * @param accountId logical account id
   * @param n how many to return (defaults to {@value TOP_N} if <= 0)
   * @return list of hashtags, highest frequency first
   */
  public List<String> topHashtags(final String accountId, final int n) {
    final List<Tweet> tweets = safeFetch(accountId, DEFAULT_FETCH);
    final Map<String, Integer> freq = new HashMap<>();

    for (Tweet t : tweets) {
      for (String token : t.text().split("\\s+")) {
        if (token.startsWith("#") && token.length() > 1) {
          final String tag = token.toLowerCase();
          freq.put(tag, freq.getOrDefault(tag, 0) + 1);
        }
      }
    }

    final int take = n <= 0 ? TOP_N : n;
    return freq.entrySet()
        .stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(take)
        .map(Map.Entry::getKey)
        .toList();
  }

  /**
   * Compute the best posting hour (UTC) based on tweet counts per hour.
   *
   * @param accountId logical account id
   * @return map of hour(0..23) to count, sorted by hour
   */
  public Map<Integer, Integer> bestHours(final String accountId) {
    final List<Tweet> tweets = safeFetch(accountId, DEFAULT_FETCH);
    final Map<Integer, Integer> byHour = new HashMap<>();

    for (Tweet t : tweets) {
      final int hour = t.createdAt().atOffset(ZoneOffset.UTC).getHour();
      byHour.put(hour, byHour.getOrDefault(hour, 0) + 1);
    }

    final List<Map.Entry<Integer, Integer>> entries =
        new ArrayList<>(byHour.entrySet());
    entries.sort(Comparator.comparingInt(Map.Entry::getKey));

    final Map<Integer, Integer> ordered = new HashMap<>();
    for (Map.Entry<Integer, Integer> e : entries) {
      ordered.put(e.getKey(), e.getValue());
    }
    return ordered;
  }

  /**
   * Roll-up summary for dashboards.
   *
   * @param accountId logical account id
   * @return summary record
   */
  public Summary summary(final String accountId) {
    final List<Tweet> tweets = safeFetch(accountId, DEFAULT_FETCH);
    final List<String> top = topHashtags(accountId, TOP_N);

    int bestHour = -1;
    int bestCount = -1;
    final Map<Integer, Integer> byHour = bestHours(accountId);
    for (Map.Entry<Integer, Integer> e : byHour.entrySet()) {
      final int hour = e.getKey();
      final int cnt = e.getValue();
      if (cnt > bestCount || cnt == bestCount && hour < bestHour) {
        bestHour = hour;
        bestCount = cnt;
      }
    }

    return new Summary(tweets.size(), top, bestHour);
  }

  // ----- helpers -----

  /**
   * Safe wrapper that attempts DB-backed fetch when enabled, and falls back
   * to the timeline client. Never throws and never returns {@code null}.
   *
   * @param accountId the logical account id used to scope tweets
   * @param count the maximum number of tweets to fetch
   * @return a non-null list of tweets; empty on failure
   */
  private List<Tweet> safeFetch(final String accountId, final int count) {
    if ("db".equalsIgnoreCase(source) && jdbc != null) {
      try {
        return fetchFromDb(accountId, count);
      } catch (DataAccessException ex) {
        // DB unavailable -> fall back immediately to timeline
        try {
          return twitter.getHomeTimeline(accountId, count);
        } catch (TwitterException ex2) {
          return List.of();
        }
      }
    }
    try {
      return twitter.getHomeTimeline(accountId, count);
    } catch (TwitterException ex) {
      return List.of();
    }
  }

  /**
   * Fetch tweets for an account from the {@code TWEETS} table.
   *
   * @param accountId the logical account id
   * @param count maximum number of rows to return
   * @return tweets ordered by {@code created_at} descending
   */
  private List<Tweet> fetchFromDb(final String accountId, final int count) {
    final String sql =
        "SELECT id, user_handle, text, created_at "
            + "FROM tweets "
            + "WHERE account_id = ? "
            + "ORDER BY created_at DESC "
            + "LIMIT ?";
    return jdbc.query(sql, (ResultSet rs, int rowNum) -> mapRow(rs),
        accountId, count);
  }

  /**
   * Map a single {@link ResultSet} row to a {@link Tweet} DTO.
   *
   * @param rs the JDBC result set positioned on a row
   * @return the mapped tweet
   * @throws SQLException if a column cannot be read
   */
  private static Tweet mapRow(final ResultSet rs) throws SQLException {
    final String id = rs.getString("id");
    final String user = rs.getString("user_handle");
    final String text = rs.getString("text");
    final Instant created = rs.getTimestamp("created_at").toInstant();
    return new Tweet(id, user, text, created);
  }

  /**
   * Compact analytics summary record.
   *
   * @param totalTweets the number of tweets considered
   * @param topHashtags the most frequent hashtags (descending)
   * @param bestHourUtc hour of day (0..23) with the highest volume
   */
  public record Summary(
      int totalTweets,
      List<String> topHashtags,
      int bestHourUtc
  ) { }
}
