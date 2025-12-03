package com.team.mcp.scheduling;

import static com.team.mcp.scheduling.ScheduledPost.Status.FAILED;
import static com.team.mcp.scheduling.ScheduledPost.Status.PENDING;
import static com.team.mcp.scheduling.ScheduledPost.Status.POSTED;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ScheduledPost entity.
 */
class ScheduledPostTest {

  @Test
  void constructor_setsFields_andDefaultsToPending() {
    Instant runAt = Instant.parse("2025-10-01T12:10:00Z");
    ScheduledPost post = new ScheduledPost("acct-1", "Hello", runAt);

    assertNull(post.getId());                    // not persisted in unit test
    assertEquals("acct-1", post.getAccountId());
    assertEquals("Hello", post.getText());
    assertEquals(runAt, post.getRunAt());
    assertEquals(PENDING, post.getStatus());
    assertNull(post.getPostedTweetId());
    assertNull(post.getCreatedAt());
    assertNull(post.getUpdatedAt());
  }

  @Test
  void markPosted_setsStatusAndTweetId() {
    ScheduledPost post = new ScheduledPost(
        "acct", "Hi", Instant.parse("2025-10-01T12:00:00Z"));

    post.markPosted("tw-123");

    assertEquals(POSTED, post.getStatus());
    assertEquals("tw-123", post.getPostedTweetId());
  }

  @Test
  void markFailed_setsStatusFailed() {
    ScheduledPost post = new ScheduledPost(
        "acct", "Hi", Instant.parse("2025-10-01T12:00:00Z"));

    post.markFailed();

    assertEquals(FAILED, post.getStatus());
  }

  @Test
  void setters_updateFields() {
    Instant runAt = Instant.parse("2025-10-01T12:10:00Z");
    ScheduledPost post = new ScheduledPost("acct-1", "Hello", runAt);

    post.setAccountId("acct-2");
    post.setText("Updated");
    post.setRunAt(runAt.plusSeconds(60));
    post.setStatus(POSTED);
    post.setPostedTweetId("tw-999");

    assertEquals("acct-2", post.getAccountId());
    assertEquals("Updated", post.getText());
    assertEquals(runAt.plusSeconds(60), post.getRunAt());
    assertEquals(POSTED, post.getStatus());
    assertEquals("tw-999", post.getPostedTweetId());
  }

  @Test
  void lifecycleCallbacks_setTimestamps() throws Exception {
    ScheduledPost post = new ScheduledPost(
        "acct", "Hi", Instant.parse("2025-10-01T12:00:00Z"));

    // Call @PrePersist
    Method onCreate =
        ScheduledPost.class.getDeclaredMethod("onCreate");
    onCreate.setAccessible(true);
    onCreate.invoke(post);

    assertNotNull(post.getCreatedAt());
    assertNotNull(post.getUpdatedAt());

    // Call @PreUpdate
    Method onUpdate =
        ScheduledPost.class.getDeclaredMethod("onUpdate");
    onUpdate.setAccessible(true);
    onUpdate.invoke(post);

    assertNotNull(post.getUpdatedAt());
    // updatedAt should not be before createdAt
    assertFalse(post.getUpdatedAt().isBefore(post.getCreatedAt()));
  }
}

