package com.team.mcp.twitter;

import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory, deterministic {@link TwitterClient} used in local runs and tests.
 *
 * <p>Behavior:
 * <ul>
 *   <li>{@code getHomeTimeline(...)} returns exactly {@code count} tweets
 *       (up to the seed size).</li>
 *   <li>{@code postTweet(...)} returns a synthetic id and does not throw.</li>
 * </ul>
 */
public final class FakeTwitterClient implements TwitterClient {

  /** Total number of seed tweets generated at startup. */
  private static final int SEED_SIZE = 200;

  /** Number of distinct users used to label seed tweets (user0..user4). */
  private static final int USERS_MOD = 5;

  /** Starting value for synthetic tweet ids (prefix "tw-"). */
  private static final long START_ID = 1000L;

  /** Pre-generated timeline content, newest first for simplicity. */
  private final List<Tweet> seed;

  /** Sequence used to generate unique synthetic tweet ids. */
  private final AtomicLong idSeq = new AtomicLong(START_ID);

  /** Builds a fake client with a fixed seed of tweets. */
  public FakeTwitterClient() {
    this.seed = new ArrayList<>(SEED_SIZE);
    final Instant base = Instant.parse("2025-01-01T00:00:00Z");
    for (int i = 0; i < SEED_SIZE; i++) {
      final String id = "seed-" + i;
      final String user = "user" + (i % USERS_MOD);
      final String text = "Hello from seed tweet #" + i;
      // Order matches record Tweet(id, user, text, createdAt).
      this.seed.add(new Tweet(id, user, text, base.plusSeconds(i)));
    }
  }

  /**
   * Returns up to {@code count} seed tweets for the given account.
   *
   * @param accountId logical account id/handle (ignored in this fake)
   * @param count maximum number of tweets to return
   * @return a new list with at most {@code count} items
   */
  @Override
  public List<Tweet> getHomeTimeline(final String accountId, final int count) {
    final int n = Math.max(0, Math.min(count, seed.size()));
    // Return a defensive copy so callers cannot mutate the seed.
    return new ArrayList<>(seed.subList(0, n));
  }

  /**
   * Pretends to post a tweet and returns a synthetic platform id.
   *
   * @param accountId logical account id/handle (ignored in this fake)
   * @param text tweet content (ignored in this fake)
   * @return a synthetic id like {@code tw-1001}
   */
  @Override
  public String postTweet(final String accountId, final String text) {
    final long n = idSeq.incrementAndGet();
    return "tw-" + n;
  }
}
