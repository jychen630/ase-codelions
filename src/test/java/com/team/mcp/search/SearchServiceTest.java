package com.team.mcp.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team.mcp.twitter.FakeTwitterClient;
import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.dto.Tweet;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SearchServiceTest {

  private final TwitterClient fake = new FakeTwitterClient();
  private final SearchService svc = new SearchService(fake);

  @Test
  void findsPhrase() {
    final List<Tweet> r = svc.search("acctA", "\"seed tweet #12\"", 0, 5);
    assertFalse(r.isEmpty());
    assertTrue(r.get(0).text().contains("#12"));
  }

  @Test
  void orMatchesMore() {
    final List<Tweet> r = svc.search("acctA", "hello OR #52", 0, 10);
    assertFalse(r.isEmpty());
  }
}
