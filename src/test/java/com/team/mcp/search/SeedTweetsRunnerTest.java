package com.team.mcp.search;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for SeedTweetsRunner (seed profile startup behavior).
 */
class SeedTweetsRunnerTest {

  @Test
  void run_invokesIngestorWithExpectedArgs() {
    TweetIngestor ingestor = mock(TweetIngestor.class);
    try {
      when(ingestor.ingestFromTimeline("acctA", 200)).thenReturn(42);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    SeedTweetsRunner runner = new SeedTweetsRunner(ingestor);

    assertDoesNotThrow(() -> runner.run());

    try {
      verify(ingestor).ingestFromTimeline("acctA", 200);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

