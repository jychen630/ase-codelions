package com.team.mcp.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds the database from current {@link com.team.mcp.twitter.TwitterClient}
 * on application startup (when the {@code seed} profile is active).
 *
 * <p>Enable with: <code>--spring.profiles.active=seed</code></p>
 */
@Profile("seed")
@Component
public final class SeedTweetsRunner implements CommandLineRunner {

  /** Logger for seed status messages. */
  private static final Logger LOG =
      LoggerFactory.getLogger(SeedTweetsRunner.class);

  /** Utility that reads timeline tweets and writes them into the DB. */
  private final TweetIngestor ingestor;

  /**
   * Constructs the runner.
   *
   * @param tweetIngestor component used to ingest tweets into the DB
   */
  public SeedTweetsRunner(final TweetIngestor tweetIngestor) {
    this.ingestor = tweetIngestor;
  }

  /**
   * Executes once at startup and ingests a fixed number of timeline tweets
   * for a demo account into the database.
   *
   * @param args startup arguments (unused)
   * @throws Exception if ingestion fails unexpectedly
   */
  @Override
  public void run(final String... args) throws Exception {
    final int n = ingestor.ingestFromTimeline("acctA", 200);
    LOG.info("Seeded {} tweets from timeline into DB", n);
  }
}
