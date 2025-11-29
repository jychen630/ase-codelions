package com.team.mcp.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.team.mcp.search.SearchService;
import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SearchTweetsTool.
 */
class SearchTweetsToolTest {

  private SearchService searchService;
  private SearchTweetsTool tool;

  @BeforeEach
  void setUp() {
    searchService = mock(SearchService.class);
    tool = new SearchTweetsTool(searchService);
  }

  @Test
  void missingArgs_returnsError() {
    var out = tool.call(Map.of());
    String text = (String) out.get(0).get("text");
    assertTrue(text.contains("accountId"));
    assertTrue(text.contains("q"));
  }

  @Test
  void emptyResults_returnsNoMatchesText() {
    when(searchService.search(anyString(), anyString(), anyInt(), anyInt()))
        .thenReturn(List.of());

    var out = tool.call(Map.of(
        "accountId", "acctA",
        "q", "nothing here"
    ));

    String text = (String) out.get(0).get("text");
    assertTrue(text.toLowerCase().contains("no matches"));
  }

  @Test
  void results_areRenderedAsTextRows() {
    var base = Instant.parse("2025-01-01T00:00:00Z");
    var t1 = new Tweet("id-1", "alice", "hello", base);
    var t2 = new Tweet("id-2", "bob", "world", base.plusSeconds(1));

    when(searchService.search("acctA", "hello", 3, 2))
        .thenReturn(List.of(t1, t2));

    var out = tool.call(Map.of(
        "accountId", "acctA",
        "q", "hello",
        "offset", 3,   // Number case
        "limit", "2"   // String case exercises parsing branch
    ));

    String text = (String) out.get(0).get("text");
    // expect both tweets represented in single text block
    assertTrue(text.contains("id-1 | alice | hello"));
    assertTrue(text.contains("id-2 | bob | world"));
  }

  @Test
  void invalidNumericArgs_fallBackToDefaults() {
    // We don't care about exact offset/limit here, only that parsing doesn't blow up
    when(searchService.search(anyString(), anyString(), anyInt(), anyInt()))
        .thenReturn(List.of());

    var out = tool.call(Map.of(
        "accountId", "acctB",
        "q", "something",
        "offset", "not-a-number",
        "limit", "also-bad"
    ));

    String text = (String) out.get(0).get("text");
    // no results, so we still get "no matches"
    assertTrue(text.toLowerCase().contains("no matches"));
  }

  @Test
  void metadata_nameAndDescription_areStable() {
    assertEquals("search_tweets", tool.name());
    assertTrue(tool.description().toLowerCase().contains("search timeline"));
  }
}

