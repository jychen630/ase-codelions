package com.team.mcp.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Simple tests for TweetEntity getters/constructor.
 */
class TweetEntityTest {

  @Test
  void constructor_setsFieldsAndGettersReturnThem() {
    Instant ts = Instant.parse("2025-01-01T00:00:00Z");
    TweetEntity e = new TweetEntity("id-123", "alice", "hello", ts);

    assertEquals("id-123", e.getId());
    assertEquals("alice", e.getUserHandle());
    assertEquals("hello", e.getText());
    assertEquals(ts, e.getCreatedAt());
  }
}

