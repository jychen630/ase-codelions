package com.team.mcp.twitter;

import static org.junit.jupiter.api.Assertions.*;

import com.team.mcp.twitter.dto.Tweet;
import java.util.List;
import org.junit.jupiter.api.Test;

class FakeTwitterClientTest {

  @Test
  void getHomeTimeline_returnsRequestedCount() {
    FakeTwitterClient client = new FakeTwitterClient();
    
    List<Tweet> tweets = client.getHomeTimeline("test-account", 10);
    
    assertEquals(10, tweets.size());
    assertNotNull(tweets.get(0).id());
    assertNotNull(tweets.get(0).text());
  }

  @Test
  void getHomeTimeline_zeroCount_returnsEmpty() {
    FakeTwitterClient client = new FakeTwitterClient();
    
    List<Tweet> tweets = client.getHomeTimeline("test-account", 0);
    
    assertEquals(0, tweets.size());
  }

  @Test
  void getHomeTimeline_negativeCount_returnsEmpty() {
    FakeTwitterClient client = new FakeTwitterClient();
    
    List<Tweet> tweets = client.getHomeTimeline("test-account", -5);
    
    assertEquals(0, tweets.size());
  }

  @Test
  void getHomeTimeline_largeCount_returnsMaxAvailable() {
    FakeTwitterClient client = new FakeTwitterClient();
    
    List<Tweet> tweets = client.getHomeTimeline("test-account", 1000);
    
    assertTrue(tweets.size() <= 200); // SEED_SIZE
    assertTrue(tweets.size() > 0);
  }

  @Test
  void postTweet_returnsSyntheticId() {
    FakeTwitterClient client = new FakeTwitterClient();
    
    String id1 = client.postTweet("account1", "First tweet");
    String id2 = client.postTweet("account2", "Second tweet");
    
    assertTrue(id1.startsWith("tw-"));
    assertTrue(id2.startsWith("tw-"));
    assertNotEquals(id1, id2);
  }

  @Test
  void postTweet_incrementsIdSequence() {
    FakeTwitterClient client = new FakeTwitterClient();
    
    String id1 = client.postTweet("account", "Tweet 1");
    String id2 = client.postTweet("account", "Tweet 2");
    
    long num1 = Long.parseLong(id1.substring(3));
    long num2 = Long.parseLong(id2.substring(3));
    
    assertEquals(1, num2 - num1);
  }

  @Test
  void getHomeTimeline_returnsDefensiveCopy() {
    FakeTwitterClient client = new FakeTwitterClient();
    
    List<Tweet> tweets1 = client.getHomeTimeline("test-account", 10);
    List<Tweet> tweets2 = client.getHomeTimeline("test-account", 10);
    
    assertEquals(tweets1.size(), tweets2.size());
    // Verify they are different instances (defensive copy)
    assertNotSame(tweets1, tweets2);
  }
}

