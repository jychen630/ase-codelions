package com.team.mcp.timeline;

import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.dto.Tweet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Business logic for reading a home timeline.
 */
@Service
public class TimelineService {

  /** Minimum number of tweets to return. */
  private static final int MIN_COUNT = 1;

  /** Maximum number of tweets to return. */
  private static final int MAX_COUNT = 50;

  /** Client used to retrieve tweets. */
  private final TwitterClient twitterClient;

  /**
   * Creates a new {@code TimelineService}.
   *
   * @param client the Twitter client used for retrieval
   */
  public TimelineService(final TwitterClient client) {
    this.twitterClient = Objects.requireNonNull(client, "twitterClient");
  }

  /**
   * Returns the home timeline for an account. The requested {@code count} is
   * clamped to {@link #MIN_COUNT}..{@link #MAX_COUNT}.
   *
   * @param accountId logical account id/handle
   * @param count requested number of tweets
   * @return a list of tweets (never {@code null})
   * @throws IllegalStateException if the underlying client fails
   */
  public List<Tweet> getHomeTimeline(final String accountId, final int count) {
    final int c = clamp(count, MIN_COUNT, MAX_COUNT);
    try {
      final List<Tweet> result = twitterClient.getHomeTimeline(accountId, c);
      return result == null ? new ArrayList<>() : result;
    } catch (TwitterClient.TwitterException e) {
      // Wrap checked exception into unchecked for controller to convert to 502.
      throw new IllegalStateException("Failed to fetch home timeline", e);
    }
  }

  /**
   * Clamp a value to an inclusive range.
   *
   * @param val the value to clamp
   * @param min the inclusive lower bound
   * @param max the inclusive upper bound
   * @return {@code min} if {@code val < min}; {@code max} if {@code val > max};
   *     otherwise {@code val}
   */
  private static int clamp(final int val, final int min, final int max) {
    if (val < min) {
      return min;
    }
    if (val > max) {
      return max;
    }
    return val;
  }
}
