package com.team.mcp.timeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team.mcp.twitter.FakeTwitterClient;
import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.dto.Tweet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Unit tests for TimelineService. */
class TimelineServiceTest {

  private TimelineService service;

  @BeforeEach
  void setUp() {
    service = new TimelineService(new FakeTwitterClient());
  }

  @Test
  void happyPath_returnsRequestedCount_whenWithinBounds() {
    List<Tweet> tweets = service.getHomeTimeline("acc", 3);
    assertNotNull(tweets);
    assertEquals(3, tweets.size());
    assertNotNull(tweets.get(0).text());
  }

  @Test
  void zeroCount_isClampedToMin() {
    List<Tweet> tweets = service.getHomeTimeline("acc", 0);
    assertNotNull(tweets);
    assertTrue(tweets.size() >= 1);
  }

  @Test
  void hugeCount_isClampedToMax() {
    List<Tweet> tweets = service.getHomeTimeline("acc", 500);
    assertNotNull(tweets);
    assertTrue(tweets.size() <= 50);
  }

  @Test
  void clientThrows_isWrappedIntoIllegalState() throws Exception {
    TwitterClient client = mock(TwitterClient.class);
    when(client.getHomeTimeline(anyString(), anyInt()))
        .thenThrow(new TwitterClient.TwitterException("rate"));

    TimelineService svc = new TimelineService(client);

    assertThrows(IllegalStateException.class,
        () -> svc.getHomeTimeline("acc", 3));
  }
}
