package com.team.mcp.search;

import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.dto.Tweet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapts a {@link TwitterClient} (Fake now, Real later) to DB storage.
 *
 * <p>Design-for-extension note:
 * This type is intentionally non-final because Spring creates a transactional
 * proxy around it. Keep public methods non-final to allow proxying.
 *
 * <p>Call {@link #ingestFromTimeline(String, int)} whenever you want to refresh
 * the cache from the timeline into the {@code tweets} table.
 */
@Service
public class TweetIngestor {

  /** Client used to fetch tweets. */
  private final TwitterClient twitter;
  /** Repository used to persist tweets. */
  private final TweetRepository repo;

  /**
   * Constructor.
   *
   * @param twitterClient injected twitter client (fake or real)
   * @param repository JPA repository for {@code tweets}
   */
  public TweetIngestor(final TwitterClient twitterClient,
                       final TweetRepository repository) {
    this.twitter = twitterClient;
    this.repo = repository;
  }

  /**
   * Fetch up to {@code count} tweets from the account's timeline and
   * persist any rows that do not already exist by id.
   *
   * @param accountId logical account id (e.g., "acctA")
   * @param count maximum number of tweets to ingest
   * @return number of newly saved rows
   * @throws TwitterClient.TwitterException if the client fails
   */
  @Transactional
  public int ingestFromTimeline(final String accountId, final int count)
      throws TwitterClient.TwitterException {
    final List<Tweet> tweets = twitter.getHomeTimeline(accountId, count);
    int saved = 0;
    for (Tweet t : tweets) {
      if (!repo.existsById(t.id())) {
        repo.save(new TweetEntity(
            t.id(), t.user(), t.text(), t.createdAt()));
        saved++;
      }
    }
    return saved;
  }
}
