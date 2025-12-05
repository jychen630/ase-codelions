package com.team.mcp.posts;

import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.TwitterClient.TwitterException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP endpoints for post management (edit and delete).
 */
@RestController
public final class PostController {

  /** Twitter client for API operations. */
  private final TwitterClient twitterClient;

  /**
   * Constructs the controller.
   *
   * @param client injected Twitter client (MastodonClient or FakeTwitterClient)
   */
  @Autowired
  public PostController(final TwitterClient client) {
    this.twitterClient = client;
  }

  /**
   * Delete a post/status by ID.
   *
   * @param statusId Mastodon status ID
   * @param accountId logical account id
   * @return HTTP 200 with success message, or error response
   */
  @DeleteMapping("/posts/{statusId}")
  public ResponseEntity<Map<String, Object>> deletePost(
      @PathVariable final String statusId,
      @RequestParam("accountId") final String accountId) {

    try {
      twitterClient.deleteStatus(accountId, statusId);
      return ResponseEntity.ok(Map.of(
          "status", "deleted",
          "id", statusId,
          "accountId", accountId));
    } catch (TwitterException ex) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", ex.getMessage(),
          "id", statusId));
    }
  }

  /**
   * Edit a post/status by ID.
   *
   * @param statusId Mastodon status ID
   * @param accountId logical account id
   * @param body request body containing "text" field
   * @return HTTP 200 with updated status info, or error response
   */
  @PutMapping("/posts/{statusId}")
  public ResponseEntity<Map<String, Object>> editPost(
      @PathVariable final String statusId,
      @RequestParam("accountId") final String accountId,
      @RequestBody final Map<String, String> body) {

    final String newText = body.get("text");
    if (newText == null || newText.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", "text field is required",
          "id", statusId));
    }

    try {
      final String updatedId =
          twitterClient.editStatus(accountId, statusId, newText);
      return ResponseEntity.ok(Map.of(
          "status", "updated",
          "id", updatedId,
          "accountId", accountId,
          "text", newText));
    } catch (TwitterException ex) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", ex.getMessage(),
          "id", statusId));
    }
  }
}

