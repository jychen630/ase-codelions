package com.team.mcp.analytics;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP endpoints exposing analytics computed from the
 * {@link com.team.mcp.twitter.TwitterClient} timeline (or DB).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>/analytics/top-hashtags</li>
 *   <li>/analytics/best-hours</li>
 *   <li>/analytics/summary</li>
 *   <li>/analytics/sentiment (Iteration-2, ML-based)</li>
 * </ul>
 */
@RestController
@RequestMapping(
    path = "/analytics",
    produces = MediaType.APPLICATION_JSON_VALUE
)
public final class AnalyticsController {

  /** Service that performs analytics. */
  private final AnalyticsService svc;

  /**
   * Creates the controller.
   *
   * @param service analytics service
   */
  public AnalyticsController(final AnalyticsService service) {
    this.svc = Objects.requireNonNull(service, "service");
  }

  /**
   * Return the top N hashtags for the account's home timeline.
   *
   * @param accountId logical account id
   * @param n number of items to return (defaults to 5 if &lt;= 0)
   * @return list of hashtags sorted by frequency
   */
  @GetMapping("/top-hashtags")
  public ResponseEntity<List<String>> topHashtags(
      @RequestParam("accountId") final String accountId,
      @RequestParam(name = "n", required = false, defaultValue = "5")
      final int n) {

    final List<String> tags = svc.topHashtags(accountId, n);
    return ResponseEntity.ok(tags);
  }

  /**
   * Return counts of tweets per UTC hour for the account timeline.
   *
   * @param accountId logical account id
   * @return map of hour (0..23) to count
   */
  @GetMapping("/best-hours")
  public ResponseEntity<Map<Integer, Integer>> bestHours(
      @RequestParam("accountId") final String accountId) {

    final Map<Integer, Integer> byHour = svc.bestHours(accountId);
    return ResponseEntity.ok(byHour);
  }

  /**
   * Return a compact analytics summary.
   *
   * @param accountId logical account id
   * @return summary with total tweets, top hashtags, best hour
   */
  @GetMapping("/summary")
  public ResponseEntity<AnalyticsService.Summary> summary(
      @RequestParam("accountId") final String accountId) {

    final AnalyticsService.Summary out = svc.summary(accountId);
    return ResponseEntity.ok(out);
  }

  /**
   * ML-based sentiment summary over recent tweets.
   *
   * <p>Example:
   * <pre>
   *   GET /analytics/sentiment?accountId=test-account
   * </pre>
   *
   * @param accountId logical account id whose tweets to analyze
   * @return sentiment summary with counts and average sentiment score
   */
  @GetMapping("/sentiment")
  public ResponseEntity<AnalyticsService.SentimentSummary> sentiment(
      @RequestParam("accountId") final String accountId) {

    final AnalyticsService.SentimentSummary out =
        svc.sentimentSummary(accountId);
    return ResponseEntity.ok(out);
  }
}

