package com.team.mcp.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team.mcp.twitter.FakeTwitterClient;
import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Tests for advanced search filters (date range, author, media).
 */
class SearchServiceFilterTest {

  @Test
  void search_withDateRangeFilter() {
    FakeTwitterClient fake = new FakeTwitterClient();
    SearchService service = new SearchService(fake);

    // Search with date range
    String from = "2025-01-01T00:00:05Z";
    String to = "2025-01-01T00:00:15Z";

    var results = service.search("test", "Hello", 0, 100,
        from, to, null, null);

    // Should only include tweets within date range
    for (Tweet t : results) {
      assertTrue(t.createdAt().isAfter(Instant.parse(from).minusSeconds(1)));
      assertTrue(t.createdAt().isBefore(Instant.parse(to).plusSeconds(1)));
    }
  }

  @Test
  void search_withAuthorFilter() {
    FakeTwitterClient fake = new FakeTwitterClient();
    SearchService service = new SearchService(fake);

    // Search for tweets by user0
    var results = service.search("test", "Hello", 0, 100,
        null, null, "user0", null);

    // All results should be from user0
    for (Tweet t : results) {
      assertEquals("user0", t.user());
    }
  }

  @Test
  void search_withMediaFilter() {
    FakeTwitterClient fake = new FakeTwitterClient();
    SearchService service = new SearchService(fake);

    // Search for tweets with media (every 10th tweet)
    var results = service.search("test", "Hello", 0, 100,
        null, null, null, true);

    // All results should have media
    for (Tweet t : results) {
      assertTrue(t.hasMedia() != null && t.hasMedia());
    }
  }

  @Test
  void search_withAllFilters() {
    FakeTwitterClient fake = new FakeTwitterClient();
    SearchService service = new SearchService(fake);

    String from = "2025-01-01T00:00:00Z";
    String to = "2025-01-01T00:00:20Z";

    var results = service.search("test", "Hello", 0, 100,
        from, to, "user0", true);

    // Results should match all filters
    for (Tweet t : results) {
      assertEquals("user0", t.user());
      assertTrue(t.hasMedia() != null && t.hasMedia());
      assertTrue(t.createdAt().isAfter(Instant.parse(from).minusSeconds(1)));
      assertTrue(t.createdAt().isBefore(Instant.parse(to).plusSeconds(1)));
    }
  }
}

