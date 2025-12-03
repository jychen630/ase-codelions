package com.team.mcp.twitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.team.mcp.twitter.dto.Tweet;
import java.util.List;
import org.junit.jupiter.api.Test;

class FakeTwitterClientTest {

  @Test
  void getHomeTimeline_returnsRequestedCount_andDefensiveCopy() {
    FakeTwitterClient client = new FakeTwitterClient();

    List<Tweet> first = client.getHomeTimeline("acct", 5);
    assertEquals(5, first.size());
    assertEquals("seed-0", first.get(0).id());
    assertEquals("seed-4", first.get(4).id());

    // Mutate returned list and ensure original seed is unaffected
    first.clear();
    List<Tweet> second = client.getHomeTimeline("acct", 5);
    assertEquals(5, second.size(), "should not be impacted by client");
  }

  @Test
  void getHomeTimeline_clampsCountWithinBounds() {
    FakeTwitterClient client = new FakeTwitterClient();

    // Negative -> 0
    List<Tweet> negative = client.getHomeTimeline("acct", -10);
    assertEquals(0, negative.size(), "negative count clamps to 0");

    // Very large -> seed size (200)
    List<Tweet> huge = client.getHomeTimeline("acct", 1000);
    assertEquals(200, huge.size(), "large count clamps to seed size");
  }

  @Test
  void postTweet_generatesIncrementingSyntheticIds() {
    FakeTwitterClient client = new FakeTwitterClient();

    String id1 = client.postTweet("acct", "hello");
    String id2 = client.postTweet("acct", "world");
    String id3 = client.postTweet("acct", "again");

    assertEquals("tw-1001", id1);
    assertEquals("tw-1002", id2);
    assertEquals("tw-1003", id3);
  }
}

