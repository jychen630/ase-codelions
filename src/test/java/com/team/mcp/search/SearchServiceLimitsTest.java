package com.team.mcp.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests clamping of offset/limit and invalid hashtag handling.
 */
final class SearchServiceLimitsTest {

  @Test
  void negativeOffset_isClampedToZero() throws Exception {
    TwitterClient tw = mock(TwitterClient.class);
    var base = Instant.parse("2025-01-01T00:00:00Z");
    var tweets = List.of(
        new Tweet("t1", "u", "hello world", base),
        new Tweet("t2", "u", "hello world again", base.plusSeconds(1))
    );
    when(tw.getHomeTimeline("acctA", 200)).thenReturn(tweets);

    SearchService svc = new SearchService(tw);
    // offset negative; limit small
    List<Tweet> result = svc.search("acctA", "hello", -5, 1);

    assertEquals(1, result.size());
  }

  @Test
  void nonPositiveLimit_defaultsAndIsClamped() throws Exception {
    TwitterClient tw = mock(TwitterClient.class);
    var base = Instant.parse("2025-01-01T00:00:00Z");
    // create a few matching tweets
    var tweets = List.of(
        new Tweet("t1", "u", "hello", base),
        new Tweet("t2", "u", "hello", base.plusSeconds(1)),
        new Tweet("t3", "u", "hello", base.plusSeconds(2))
    );
    when(tw.getHomeTimeline("acctA", 200)).thenReturn(tweets);

    SearchService svc = new SearchService(tw);
    // limit <= 0 should fall back to default and still clamp to MAX_LIMIT
    List<Tweet> result = svc.search("acctA", "hello", 0, 0);

    assertTrue(result.size() >= 1);
  }

  @Test
  void searchHashtag_invalidPrefix_returnsEmptyImmediately() throws Exception {
    TwitterClient tw = mock(TwitterClient.class);
    SearchService svc = new SearchService(tw);

    List<Tweet> result = svc.searchHashtag("acctA", "not-a-hash", 10);
    assertTrue(result.isEmpty());
  }
}

