package com.team.mcp.analytics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.TwitterClient.TwitterException;
import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Covers AnalyticsService (timeline + DB + sentiment).
 */
final class AnalyticsServiceTest {

  @Test
  void topHashtags_bestHours_summary_work() throws Exception {
    // 3 tweets in different hours, with hashtags
    Instant base = Instant.parse("2025-01-01T00:00:00Z");
    List<Tweet> tweets = List.of(
        new Tweet("a", "u1", "#x hello", base.plusSeconds(10)),
        new Tweet("b", "u2", "#x #y hi", base.plusSeconds(3700)), // next hour
        new Tweet("c", "u3", "no tag", base.plusSeconds(7200))    // another hour
    );

    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acctA", 100)).thenReturn(tweets);

    AnalyticsService svc = new AnalyticsService(tw, null, "memory");

    // top hashtags
    List<String> tags = svc.topHashtags("acctA", 5);
    assertEquals(List.of("#x", "#y"), tags); // #x appears twice, #y once

    // hour distribution
    Map<Integer, Integer> byHour = svc.bestHours("acctA");
    assertTrue(byHour.size() >= 2);

    // summary
    AnalyticsService.Summary sum = svc.summary("acctA");
    assertEquals(3, sum.totalTweets());
    assertEquals("#x", sum.topHashtags().get(0));
    assertTrue(sum.bestHourUtc() >= 0 && sum.bestHourUtc() <= 23);
  }

  @Test
  void topHashtags_defaultsNAndIgnoresNonHashtags() throws Exception {
    Instant now = Instant.parse("2025-01-01T00:00:00Z");
    List<Tweet> tweets = List.of(
        new Tweet("1", "u", "hello world", now),
        new Tweet("2", "u", "#Tag #TAG", now)
    );

    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acct", 100)).thenReturn(tweets);

    AnalyticsService svc = new AnalyticsService(tw, null, "timeline");

    List<String> tags = svc.topHashtags("acct", 0); // n <= 0 -> default
    assertEquals(1, tags.size());
    assertEquals("#tag", tags.get(0)); // normalized + counted once
  }

  @Test
  void bestHours_handlesEmptyTweetList() throws Exception {
    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acct", 100)).thenReturn(List.of());

    AnalyticsService svc = new AnalyticsService(tw, null, "timeline");

    Map<Integer, Integer> byHour = svc.bestHours("acct");
    assertTrue(byHour.isEmpty());
  }

  @Test
  void summary_withMultipleHours_runsWithoutError() throws Exception {
    Instant base = Instant.parse("2025-01-01T00:00:00Z");
    List<Tweet> tweets = List.of(
        new Tweet("a", "u", "t", base.plusSeconds(10)),        // hour 0
        new Tweet("b", "u", "t", base.plusSeconds(20)),        // hour 0
        new Tweet("c", "u", "t", base.plusSeconds(3600)),      // hour 1
        new Tweet("d", "u", "t", base.plusSeconds(3660))       // hour 1
    );

    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acct", 100)).thenReturn(tweets);

    AnalyticsService svc = new AnalyticsService(tw, null, "timeline");

    AnalyticsService.Summary sum = svc.summary("acct");
    assertEquals(4, sum.totalTweets());
    assertTrue(sum.bestHourUtc() >= 0 && sum.bestHourUtc() <= 23);
  }

  @Test
  void sentimentSummary_countsLabelsAndAverage() throws Exception {
    Instant now = Instant.parse("2025-01-01T00:00:00Z");
    List<Tweet> tweets = List.of(
        new Tweet("1", "u", "this is good", now),
        new Tweet("2", "u", "this is bad", now.plusSeconds(10)),
        new Tweet("3", "u", "meh", now.plusSeconds(20))
    );

    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acct", 100)).thenReturn(tweets);

    AnalyticsService svc = new AnalyticsService(tw, null, "timeline");

    AnalyticsService.SentimentSummary s = svc.sentimentSummary("acct");
    assertEquals(3, s.totalTweets());
    assertEquals(1, s.positive());
    assertEquals(1, s.negative());
    assertEquals(1, s.neutral());
    assertEquals(0.0, s.averageScore(), 1e-9);
  }

  @Test
  void sentimentSummary_noTweets_returnsZeroes() throws Exception {
    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acct", 100)).thenReturn(List.of());

    AnalyticsService svc = new AnalyticsService(tw, null, "timeline");

    AnalyticsService.SentimentSummary s = svc.sentimentSummary("acct");
    assertEquals(0, s.totalTweets());
    assertEquals(0, s.positive());
    assertEquals(0, s.negative());
    assertEquals(0, s.neutral());
    assertEquals(0.0, s.averageScore(), 1e-9);
  }

  @Test
  void safeFetch_dbSourceSuccess_usesJdbc() throws Exception {
    TwitterClient tw = mock(TwitterClient.class);

    FakeJdbcTemplate jdbc = new FakeJdbcTemplate();
    Instant now = Instant.parse("2025-01-01T00:00:00Z");
    List<Tweet> dbTweets = List.of(
        new Tweet("db1", "u", "from db", now)
    );
    jdbc.toReturn = dbTweets;

    AnalyticsService svc = new AnalyticsService(tw, jdbc, "db");

    @SuppressWarnings("unchecked")
    List<Tweet> result = (List<Tweet>) ReflectionTestUtils.invokeMethod(
        svc, "safeFetch", "acct", 10);

    assertEquals(dbTweets, result);
    verify(tw, never()).getHomeTimeline(anyString(), anyInt());
  }

  @Test
  void safeFetch_dbSourceDataAccess_fallsBackToTwitter() throws Exception {
    TwitterClient tw = mock(TwitterClient.class);

    FakeJdbcTemplate jdbc = new FakeJdbcTemplate();
    jdbc.toThrow = new DataAccessResourceFailureException("db down");

    Instant now = Instant.parse("2025-01-01T00:00:00Z");
    List<Tweet> fallback = List.of(
        new Tweet("t1", "u", "from timeline", now)
    );
    when(tw.getHomeTimeline("acct", 10)).thenReturn(fallback);

    AnalyticsService svc = new AnalyticsService(tw, jdbc, "db");

    @SuppressWarnings("unchecked")
    List<Tweet> result = (List<Tweet>) ReflectionTestUtils.invokeMethod(
        svc, "safeFetch", "acct", 10);

    assertEquals(fallback, result);
  }

  @Test
  void safeFetch_dbAndTwitterFail_returnsEmptyList() throws Exception {
    TwitterClient tw = mock(TwitterClient.class);

    FakeJdbcTemplate jdbc = new FakeJdbcTemplate();
    jdbc.toThrow = new DataAccessResourceFailureException("db down");

    when(tw.getHomeTimeline(anyString(), anyInt()))
        .thenThrow(new TwitterException("net down"));

    AnalyticsService svc = new AnalyticsService(tw, jdbc, "db");

    @SuppressWarnings("unchecked")
    List<Tweet> result = (List<Tweet>) ReflectionTestUtils.invokeMethod(
        svc, "safeFetch", "acct", 10);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void safeFetch_timelineTwitterFails_returnsEmptyList() throws Exception {
    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline(anyString(), anyInt()))
        .thenThrow(new TwitterException("net down"));

    AnalyticsService svc = new AnalyticsService(tw, null, "timeline");

    @SuppressWarnings("unchecked")
    List<Tweet> result = (List<Tweet>) ReflectionTestUtils.invokeMethod(
        svc, "safeFetch", "acct", 10);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  /**
   * Small fake JdbcTemplate that lets us control the behaviour of
   * query(String, RowMapper, Object...) without fighting overloads.
   */
  private static final class FakeJdbcTemplate extends JdbcTemplate {

    List<?> toReturn;
    RuntimeException toThrow;

    @Override
    public <T> List<T> query(final String sql,
                             final RowMapper<T> rowMapper,
                             final Object... args) throws DataAccessException {
      if (toThrow != null) {
        throw toThrow;
      }
      @SuppressWarnings("unchecked")
      List<T> cast = (List<T>) toReturn;
      return cast;
    }
  }
}

