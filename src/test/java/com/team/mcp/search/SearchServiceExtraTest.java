package com.team.mcp.search;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.TwitterClient.TwitterException;
import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Extra coverage for SearchService: hashtag matching, pagination, and
 * client error fallback (safeFetch returns empty).
 */
final class SearchServiceExtraTest {

  @Test
  void hashtagSearch_exactMatch_caseInsensitive() throws Exception {
    TwitterClient tw = mock(TwitterClient.class);
    Instant base = Instant.parse("2025-01-01T00:00:00Z");
    List<Tweet> seed = List.of(
        new Tweet("t1","u","#Tag hello", base.plusSeconds(1)),
        new Tweet("t2","u","no hash",   base.plusSeconds(2)),
        new Tweet("t3","u","#tag rest", base.plusSeconds(3))
    );
    when(tw.getHomeTimeline("acctA", 200)).thenReturn(seed);

    SearchService svc = new SearchService(tw); // memory-mode ctor
    List<Tweet> hits = svc.searchHashtag("acctA", "#tag", 10);

    assertEquals(2, hits.size());
    assertTrue(hits.get(0).text().toLowerCase().contains("#tag"));
  }

  @Test
  void pagination_narrowWindow_respectsOffsetAndLimit() throws Exception {
    TwitterClient tw = mock(TwitterClient.class);
    Instant base = Instant.parse("2025-01-01T00:00:00Z");
    List<Tweet> seed = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      seed.add(new Tweet("t"+i, "u", "hello seed " + i, base.plusSeconds(i)));
    }
    when(tw.getHomeTimeline("acctA", 200)).thenReturn(seed);

    SearchService svc = new SearchService(tw);
    // offset=3, limit=2 -> should return two items after scoring/sort
    List<Tweet> page = svc.search("acctA", "hello seed", 3, 2);

    assertEquals(2, page.size());
  }

  @Test
  void clientFailure_returnsEmpty() throws Exception {
    TwitterClient tw = mock(TwitterClient.class);
    when(tw.getHomeTimeline("acctA", 200))
        .thenThrow(new TwitterException("boom"));

    SearchService svc = new SearchService(tw);

    assertTrue(svc.search("acctA", "anything", 0, 5).isEmpty());
    assertTrue(svc.searchHashtag("acctA", "#anything", 5).isEmpty());
  }
}
