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
}
