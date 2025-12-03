package com.team.mcp.scheduling;

import com.team.mcp.twitter.TwitterClient;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Coordinates scheduling and publishing of tweets.
 *
 * <p>Persists new scheduled posts and periodically publishes due ones using a
 * {@link TwitterClient}. Uses an injected {@link Clock} for testability.
 */
@Service
public class SchedulingService {

  /** Default maximum items to publish per tick. */
  private static final int DEFAULT_BATCH_SIZE = 50;

  /** Repository for scheduled posts. */
  private final ScheduledPostRepository repo;

  /** Client used to publish tweets. */
  private final TwitterClient twitterClient;

  /** Clock used for time (inject a fixed clock in tests). */
  private final Clock clock;

  /** Maximum items to publish per tick. */
  private final int batchSize;

  /**
   * Spring-injected constructor (preferred in production).
   *
   * @param repoParam JPA repository for scheduled posts
   * @param twitterClientParam client used to publish tweets
   * @param clockParam system clock (inject a fixed clock in tests)
   */
  @Autowired
  public SchedulingService(final ScheduledPostRepository repoParam,
                           final TwitterClient twitterClientParam,
                           final Clock clockParam) {
    this(repoParam, twitterClientParam, clockParam, DEFAULT_BATCH_SIZE);
  }

  /**
   * Testing/explicit-batch constructor. Keep public so unit tests can pass a
   * custom batch size.
   *
   * @param repoParam JPA repository for scheduled posts
   * @param twitterClientParam client used to publish tweets
   * @param clockParam system clock (inject a fixed clock in tests)
   * @param batchSizeParam maximum items to publish per tick
   */
  public SchedulingService(final ScheduledPostRepository repoParam,
                           final TwitterClient twitterClientParam,
                           final Clock clockParam,
                           final int batchSizeParam) {
    this.repo = Objects.requireNonNull(repoParam, "repo");
    this.twitterClient = Objects.requireNonNull(
        twitterClientParam, "twitterClient"
    );
    this.clock = clockParam == null ? Clock.systemUTC() : clockParam;
    this.batchSize = batchSizeParam <= 0 ? DEFAULT_BATCH_SIZE : batchSizeParam;
  }

  /**
   * Validate inputs and persist a {@link ScheduledPost} in {@code PENDING}
   * state.
   *
   * @param text tweet content
   * @param runAt time to publish (must be in the future)
   * @param accountId logical account id/handle
   * @return scheduled id as a string (convenient for API responses)
   */
  @Transactional
  public String schedule(final String text,
                         final Instant runAt,
                         final String accountId) {
    if (accountId == null || accountId.isBlank()) {
      throw new IllegalArgumentException("accountId is required");
    }
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("text is required");
    }
    if (runAt == null) {
      throw new IllegalArgumentException("time is required");
    }
    final Instant now = Instant.now(clock);
    if (!runAt.isAfter(now)) {
      throw new IllegalArgumentException("time must be in the future");
    }

    ScheduledPost entity = new ScheduledPost(accountId, text, runAt);
    entity = repo.save(entity);
    return String.valueOf(entity.getId());
  }

  /**
   * Publish due {@code PENDING} posts and update their statuses.
   *
   * @return number of posts successfully marked {@code POSTED}
   */
  @Transactional
  public int publisherTick() {
    int processed = 0;
    final Instant now = Instant.now(clock);
    final var due = repo.findDue(
        now, ScheduledPost.Status.PENDING, PageRequest.of(0, batchSize)
    );

    for (final var s : due) {
      try {
        final String tweetId =
            twitterClient.postTweet(s.getAccountId(), s.getText());
        s.markPosted(tweetId);
        processed++;
      } catch (TwitterClient.TwitterException e) {
        s.markFailed();
      }
      // JPA dirty checking persists state changes on commit.
    }
    return processed;
  }
}
