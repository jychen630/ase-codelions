package com.team.mcp.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
  void name_returnsSearchTweets() {
    assertEquals("search_tweets", tool.name());
  }

  @Test
  void description_returnsDescription() {
    assertNotNull(tool.description());
    assertTrue(tool.description().contains("Search"));
  }

  @Test
  void call_validArgs_returnsResults() {
    List<Tweet> tweets = List.of(
        new Tweet("id1", "user1", "test tweet", Instant.now())
    );
    when(searchService.search("acct1", "test", 0, 20))
        .thenReturn(tweets);

    Map<String, Object> args = Map.of(
        "accountId", "acct1",
        "q", "test"
    );

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals("text", result.get(0).get("type"));
    assertTrue(result.get(0).get("text").toString().contains("id1"));
  }

  @Test
  void call_missingAccountId_returnsError() {
    Map<String, Object> args = Map.of("q", "test");

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
  }

  @Test
  void call_missingQuery_returnsError() {
    Map<String, Object> args = Map.of("accountId", "acct1");

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
  }

  @Test
  void call_nullAccountId_returnsError() {
    Map<String, Object> args = new java.util.HashMap<>();
    args.put("accountId", null);
    args.put("q", "test");

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
  }

  @Test
  void call_blankAccountId_returnsError() {
    Map<String, Object> args = Map.of(
        "accountId", "   ",
        "q", "test"
    );

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
  }

  @Test
  void call_blankQuery_returnsError() {
    Map<String, Object> args = Map.of(
        "accountId", "acct1",
        "q", "   "
    );

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
  }

  @Test
  void call_nonStringAccountId_returnsError() {
    Map<String, Object> args = Map.of(
        "accountId", 123,
        "q", "test"
    );

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
  }

  @Test
  void call_nonStringQuery_returnsError() {
    Map<String, Object> args = Map.of(
        "accountId", "acct1",
        "q", 123
    );

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("error", result.get(0).get("text").toString().substring(0, 5));
  }

  @Test
  void call_withOffsetAndLimit_usesProvidedValues() {
    List<Tweet> tweets = List.of();
    when(searchService.search("acct1", "test", 10, 5))
        .thenReturn(tweets);

    Map<String, Object> args = Map.of(
        "accountId", "acct1",
        "q", "test",
        "offset", 10,
        "limit", 5
    );

    List<Map<String, Object>> result = tool.call(args);
    verify(searchService).search("acct1", "test", 10, 5);
  }

  @Test
  void call_withStringOffset_parsesCorrectly() {
    List<Tweet> tweets = List.of();
    when(searchService.search("acct1", "test", 5, 20))
        .thenReturn(tweets);

    Map<String, Object> args = Map.of(
        "accountId", "acct1",
        "q", "test",
        "offset", "5"
    );

    tool.call(args);
    verify(searchService).search("acct1", "test", 5, 20);
  }

  @Test
  void call_withInvalidStringOffset_usesDefault() {
    List<Tweet> tweets = List.of();
    when(searchService.search("acct1", "test", 0, 20))
        .thenReturn(tweets);

    Map<String, Object> args = Map.of(
        "accountId", "acct1",
        "q", "test",
        "offset", "invalid"
    );

    tool.call(args);
    verify(searchService).search("acct1", "test", 0, 20);
  }

  @Test
  void call_emptyResults_returnsNoMatches() {
    when(searchService.search("acct1", "test", 0, 20))
        .thenReturn(List.of());

    Map<String, Object> args = Map.of(
        "accountId", "acct1",
        "q", "test"
    );

    List<Map<String, Object>> result = tool.call(args);
    assertNotNull(result);
    assertEquals("no matches", result.get(0).get("text"));
  }

  @Test
  void call_multipleResults_formatsCorrectly() {
    List<Tweet> tweets = List.of(
        new Tweet("id1", "user1", "tweet1", Instant.now()),
        new Tweet("id2", "user2", "tweet2", Instant.now())
    );
    when(searchService.search("acct1", "test", 0, 20))
        .thenReturn(tweets);

    Map<String, Object> args = Map.of(
        "accountId", "acct1",
        "q", "test"
    );

    List<Map<String, Object>> result = tool.call(args);
    String text = result.get(0).get("text").toString();
    assertTrue(text.contains("id1"));
    assertTrue(text.contains("id2"));
    assertTrue(text.contains("user1"));
    assertTrue(text.contains("user2"));
  }
}

