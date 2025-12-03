package com.team.mcp.twitter;

import com.team.mcp.twitter.dto.Tweet;
import java.util.List;

/**
 * Abstraction over Twitter/X operations used by our services.
 *
 * <p>Implementations:
 * <ul>
 *   <li>RealTwitterClient: talks to the real Twitter API (later).</li>
 *   <li>FakeTwitterClient: deterministic, in-memory for tests/dev.</li>
 * </ul>
 */
public interface TwitterClient {

  /**
   * Posts a new tweet for a given account.
   *
   * @param accountId logical id or handle (e.g. {@code "test-account"})
   * @param text tweet content
   * @return tweet id assigned by the platform (or a fake id in tests)
   * @throws TwitterException when the post fails (rate limit,
   *     network, etc.)
   */
  String postTweet(String accountId, String text) throws TwitterException;

  /**
   * Returns the most recent tweets for the account's home timeline.
   *
   * @param accountId logical id or handle for the account
   * @param count max number of tweets to return (implementation may
   *     clamp)
   * @return list of tweets, newest first
   * @throws TwitterException on retrieval errors
   */
  List<Tweet> getHomeTimeline(String accountId, int count)
      throws TwitterException;

  /**
   * Checked exception to model client failures in a testable way.
   */
  class TwitterException extends Exception {

    /**
     * Creates a {@code TwitterException} with a message.
     *
     * @param message explanation of the failure
     */
    public TwitterException(final String message) {
      super(message);
    }

    /**
     * Creates a {@code TwitterException} with a message and cause.
     *
     * @param message explanation of the failure
     * @param cause underlying cause
     */
    public TwitterException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
