package com.team.mcp.analytics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Covers AnalyticsService: topHashtags, bestHours, summary.
 */
final class AnalyticsServiceTest {

  @Test
  void topHashtags_bestHours_summary_work() throws Exception {
    // 3 tweets in different hours, with hashtags
    Instant base = Instant.parse("2025-01-01T00:00:00Z");
    List<Tweet> tweets = List.of(
        new Tweet("a","u1","#x hello", base.plusSeconds(10)),
        new Tweet("b","u2","#x #y hi", base.plusSeconds(3700)), // next hour
        new Tweet("c","u3","no tag",  base.plusSeconds(7200))   // another hour
    );

    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acctA", 100)).thenReturn(tweets);

    // Use the 3-arg constructor: (twitterClient, jdbcTemplate, source)
    AnalyticsService svc = new AnalyticsService(tw, null, "memory");

    // top hashtags
    List<String> tags = svc.topHashtags("acctA", 5);
    assertEquals(List.of("#x", "#y"), tags); // #x appears twice, #y once

    // hour distribution
    Map<Integer,Integer> byHour = svc.bestHours("acctA");
    assertTrue(byHour.size() >= 2);

    // summary
    AnalyticsService.Summary sum = svc.summary("acctA");
    assertEquals(3, sum.totalTweets());
    assertEquals("#x", sum.topHashtags().get(0));
    assertTrue(sum.bestHourUtc() >= 0 && sum.bestHourUtc() <= 23);
  }

  @Test
  void topHashtags_zeroOrNegativeN_usesDefault() throws Exception {
    List<Tweet> tweets = List.of(
        new Tweet("a", "u1", "#tag1 #tag2 #tag3 #tag4 #tag5 #tag6", 
            Instant.parse("2025-01-01T00:00:00Z"))
    );
    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acct", 100)).thenReturn(tweets);
    AnalyticsService svc = new AnalyticsService(tw, null, "timeline");

    List<String> tagsZero = svc.topHashtags("acct", 0);
    assertEquals(5, tagsZero.size(), "should use default TOP_N=5");

    List<String> tagsNegative = svc.topHashtags("acct", -10);
    assertEquals(5, tagsNegative.size(), "should use default TOP_N=5");
  }

  @Test
  void topHashtags_noHashtags_returnsEmpty() throws Exception {
    List<Tweet> tweets = List.of(
        new Tweet("a", "u1", "no hashtags here", Instant.parse("2025-01-01T00:00:00Z"))
    );
    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acct", 100)).thenReturn(tweets);
    AnalyticsService svc = new AnalyticsService(tw, null, "timeline");

    List<String> tags = svc.topHashtags("acct", 5);
    assertTrue(tags.isEmpty());
  }

  @Test
  void topHashtags_singleCharHashtag_ignored() throws Exception {
    List<Tweet> tweets = List.of(
        new Tweet("a", "u1", "#", Instant.parse("2025-01-01T00:00:00Z"))
    );
    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acct", 100)).thenReturn(tweets);
    AnalyticsService svc = new AnalyticsService(tw, null, "timeline");

    List<String> tags = svc.topHashtags("acct", 5);
    assertTrue(tags.isEmpty(), "single char # should be ignored");
  }

  @Test
  void bestHours_tieBreaker_usesEarlierHour() throws Exception {
    Instant base = Instant.parse("2025-01-01T10:00:00Z");
    List<Tweet> tweets = List.of(
        new Tweet("a", "u1", "tweet", base),  // hour 10
        new Tweet("b", "u2", "tweet", base.plusSeconds(3600)),  // hour 11
        new Tweet("c", "u3", "tweet", base.plusSeconds(7200))   // hour 12
    );
    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acct", 100)).thenReturn(tweets);
    AnalyticsService svc = new AnalyticsService(tw, null, "timeline");

    AnalyticsService.Summary sum = svc.summary("acct");
    // All hours have same count (1), should pick earliest (10)
    assertEquals(10, sum.bestHourUtc());
  }

  @Test
  void safeFetch_twitterException_returnsEmpty() throws Exception {
    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acct", 100))
        .thenThrow(new TwitterClient.TwitterException("error"));
    AnalyticsService svc = new AnalyticsService(tw, null, "timeline");

    List<String> tags = svc.topHashtags("acct", 5);
    assertTrue(tags.isEmpty(), "should return empty on exception");
  }

  @Test
  void safeFetch_emptyTweets_handlesGracefully() throws Exception {
    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acct", 100)).thenReturn(List.of());
    AnalyticsService svc = new AnalyticsService(tw, null, "timeline");

    List<String> tags = svc.topHashtags("acct", 5);
    assertTrue(tags.isEmpty());
    
    Map<Integer, Integer> hours = svc.bestHours("acct");
    assertTrue(hours.isEmpty());
    
    AnalyticsService.Summary sum = svc.summary("acct");
    assertEquals(0, sum.totalTweets());
    assertTrue(sum.topHashtags().isEmpty());
    assertEquals(-1, sum.bestHourUtc());
  }

  @Test
  void summary_sameCountDifferentHours_picksEarlierHour() throws Exception {
    Instant base = Instant.parse("2025-01-01T15:00:00Z");
    List<Tweet> tweets = List.of(
        new Tweet("a", "u1", "tweet", base),  // hour 15
        new Tweet("b", "u2", "tweet", base.plusSeconds(3600)),  // hour 16
        new Tweet("c", "u3", "tweet", base.plusSeconds(7200))   // hour 17
    );
    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acct", 100)).thenReturn(tweets);
    AnalyticsService svc = new AnalyticsService(tw, null, "timeline");

    AnalyticsService.Summary sum = svc.summary("acct");
    // All hours have count=1, should pick earliest (15)
    assertEquals(15, sum.bestHourUtc());
  }

  @Test
  void summary_higherCountWins() throws Exception {
    Instant base = Instant.parse("2025-01-01T10:00:00Z");
    List<Tweet> tweets = List.of(
        new Tweet("a", "u1", "tweet", base),  // hour 10
        new Tweet("b", "u2", "tweet", base),  // hour 10
        new Tweet("c", "u3", "tweet", base.plusSeconds(3600))   // hour 11
    );
    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acct", 100)).thenReturn(tweets);
    AnalyticsService svc = new AnalyticsService(tw, null, "timeline");

    AnalyticsService.Summary sum = svc.summary("acct");
    // Hour 10 has count=2, hour 11 has count=1, should pick 10
    assertEquals(10, sum.bestHourUtc());
  }
}
