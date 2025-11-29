package com.team.mcp.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.TwitterClient.TwitterException;
import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TweetIngestor.
 */
class TweetIngestorTest {

  @Test
  void ingest_savesOnlyNewTweets_returnsCount() throws Exception {
    TwitterClient client = mock(TwitterClient.class);
    TweetRepository repo = mock(TweetRepository.class);

    var base = Instant.parse("2025-01-01T00:00:00Z");
    var t1 = new Tweet("id-1", "alice", "hello", base);
    var t2 = new Tweet("id-2", "bob", "hi", base.plusSeconds(1));

    when(client.getHomeTimeline("acctA", 2))
        .thenReturn(List.of(t1, t2));

    when(repo.existsById("id-1")).thenReturn(false);
    when(repo.existsById("id-2")).thenReturn(true);

    TweetIngestor ingestor = new TweetIngestor(client, repo);
    int saved = ingestor.ingestFromTimeline("acctA", 2);

    // only t1 should be saved
    assertEquals(1, saved);
    verify(repo).save(any(TweetEntity.class));
    verify(repo, times(1)).existsById("id-1");
    verify(repo, times(1)).existsById("id-2");
  }

  @Test
  void ingest_propagatesTwitterException() throws Exception {
    TwitterClient client = mock(TwitterClient.class);
    TweetRepository repo = mock(TweetRepository.class);

    when(client.getHomeTimeline("acctA", 5))
        .thenThrow(new TwitterException("boom"));

    TweetIngestor ingestor = new TweetIngestor(client, repo);

    assertThrows(TwitterException.class,
        () -> ingestor.ingestFromTimeline("acctA", 5));
  }
}

