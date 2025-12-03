package com.team.mcp.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.when;

/**
 * Integration test for {@link TweetIngestor} using H2 + JPA.
 *
 * <p>Verifies that tweets fetched from {@link TwitterClient} are
 * persisted into the {@code tweets} table via {@link TweetRepository}.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
class TweetIngestorIntegrationTest {

  @Autowired
  private TweetIngestor ingestor;

  @Autowired
  private TweetRepository repo;

  @MockBean
  private TwitterClient twitterClient;

  @Test
  void ingestFromTimeline_persistsNewTweets() throws Exception {
    // Ensure a clean table
    repo.deleteAll();

    Instant base = Instant.parse("2025-01-01T00:00:00Z");
    List<Tweet> seed = List.of(
        new Tweet("ing1", "u1", "hello 1", base),
        new Tweet("ing2", "u2", "hello 2", base.plusSeconds(1))
    );
    when(twitterClient.getHomeTimeline("acctA", 2)).thenReturn(seed);

    int saved = ingestor.ingestFromTimeline("acctA", 2);

    assertEquals(2, saved);
    assertEquals(2L, repo.count());
    assertTrue(repo.existsById("ing1"));
  }
}
