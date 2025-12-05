package com.team.mcp.posts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.TwitterClient.TwitterException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tests for PostController (edit and delete endpoints).
 */
class PostControllerTest {

  private TwitterClient twitterClient;
  private PostController controller;

  @BeforeEach
  void setUp() {
    twitterClient = mock(TwitterClient.class);
    controller = new PostController(twitterClient);
  }

  @Test
  void deletePost_success() throws Exception {
    ResponseEntity<Map<String, Object>> resp =
        controller.deletePost("123", "test-account");

    assertEquals(HttpStatus.OK, resp.getStatusCode());
    assertNotNull(resp.getBody());
    assertEquals("deleted", resp.getBody().get("status"));
    assertEquals("123", resp.getBody().get("id"));
    verify(twitterClient).deleteStatus("test-account", "123");
  }

  @Test
  void deletePost_noTokenReturnsBadRequest() throws Exception {
    doThrow(new TwitterException("No token"))
        .when(twitterClient).deleteStatus(anyString(), anyString());

    ResponseEntity<Map<String, Object>> resp =
        controller.deletePost("123", "test-account");

    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    assertNotNull(resp.getBody());
    assertEquals("No token", resp.getBody().get("error"));
  }

  @Test
  void editPost_success() throws Exception {
    when(twitterClient.editStatus(eq("test-account"), eq("123"), eq("New text")))
        .thenReturn("123");

    Map<String, String> body = Map.of("text", "New text");
    ResponseEntity<Map<String, Object>> resp =
        controller.editPost("123", "test-account", body);

    assertEquals(HttpStatus.OK, resp.getStatusCode());
    assertNotNull(resp.getBody());
    assertEquals("updated", resp.getBody().get("status"));
    assertEquals("123", resp.getBody().get("id"));
    assertEquals("New text", resp.getBody().get("text"));
    verify(twitterClient).editStatus("test-account", "123", "New text");
  }

  @Test
  void editPost_missingTextReturnsBadRequest() {
    Map<String, String> body = Map.of();
    ResponseEntity<Map<String, Object>> resp =
        controller.editPost("123", "test-account", body);

    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    assertNotNull(resp.getBody());
    assertEquals("text field is required", resp.getBody().get("error"));
  }

  @Test
  void editPost_blankTextReturnsBadRequest() {
    Map<String, String> body = Map.of("text", "   ");
    ResponseEntity<Map<String, Object>> resp =
        controller.editPost("123", "test-account", body);

    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    assertNotNull(resp.getBody());
    assertEquals("text field is required", resp.getBody().get("error"));
  }

  @Test
  void editPost_noTokenReturnsBadRequest() throws Exception {
    doThrow(new TwitterException("No token"))
        .when(twitterClient).editStatus(anyString(), anyString(), anyString());

    Map<String, String> body = Map.of("text", "New text");
    ResponseEntity<Map<String, Object>> resp =
        controller.editPost("123", "test-account", body);

    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    assertNotNull(resp.getBody());
    assertEquals("No token", resp.getBody().get("error"));
  }
}

