package com.team.mcp.search;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for SearchController.
 */
class SearchControllerTest {

  private SearchService searchService;
  private SearchController controller;

  @BeforeEach
  void setUp() {
    searchService = mock(SearchService.class);
    controller = new SearchController(searchService);
  }

  @Test
  void search_returnsTweets() {
    List<Tweet> tweets = List.of(
        new Tweet("id1", "user1", "test tweet", Instant.now())
    );
    when(searchService.search("acct", "query", 0, 20))
        .thenReturn(tweets);

    ResponseEntity<List<Tweet>> response = controller.search("acct", "query", 0, 20);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(tweets, response.getBody());
  }

  @Test
  void search_usesDefaultOffsetAndLimit() {
    List<Tweet> tweets = List.of();
    when(searchService.search("acct", "query", 0, 20))
        .thenReturn(tweets);

    ResponseEntity<List<Tweet>> response = controller.search("acct", "query", 0, 20);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(searchService).search("acct", "query", 0, 20);
  }

  @Test
  void hashtags_validHashtag_returnsTweets() {
    List<Tweet> tweets = List.of(
        new Tweet("id1", "user1", "#test tweet", Instant.now())
    );
    when(searchService.searchHashtag("acct", "#test", 20))
        .thenReturn(tweets);

    ResponseEntity<?> response = controller.hashtags("acct", "#test", 20);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    List<Tweet> body = (List<Tweet>) response.getBody();
    assertEquals(tweets, body);
  }

  @Test
  void hashtags_nullQuery_returns400() {
    ResponseEntity<?> response = controller.hashtags("acct", null, 20);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    @SuppressWarnings("unchecked")
    java.util.Map<String, String> body = (java.util.Map<String, String>) response.getBody();
    assertNotNull(body);
    assertEquals("query must start with '#'", body.get("error"));
    verifyNoInteractions(searchService);
  }

  @Test
  void hashtags_queryWithoutHash_returns400() {
    ResponseEntity<?> response = controller.hashtags("acct", "nohash", 20);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    @SuppressWarnings("unchecked")
    java.util.Map<String, String> body = (java.util.Map<String, String>) response.getBody();
    assertNotNull(body);
    assertEquals("query must start with '#'", body.get("error"));
    verifyNoInteractions(searchService);
  }

  @Test
  void hashtags_usesDefaultLimit() {
    List<Tweet> tweets = List.of();
    when(searchService.searchHashtag("acct", "#test", 20))
        .thenReturn(tweets);

    ResponseEntity<?> response = controller.hashtags("acct", "#test", 20);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(searchService).searchHashtag("acct", "#test", 20);
  }
}

