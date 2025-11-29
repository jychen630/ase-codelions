package com.team.mcp.tools;

import com.team.mcp.security.TokenProvider;
import com.team.mcp.scheduling.SchedulingService;
import com.team.mcp.timeline.TimelineService;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal tools API used by the front-end / tests for Iteration-1.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>/tools/get_home_timeline — returns recent tweets</li>
 *   <li>/tools/schedule_tweet — schedules a tweet for a future time</li>
 * </ul>
 */
@RestController
@RequestMapping("/tools")
public class ToolsController {

  /** Default count when the client does not specify one. */
  private static final int DEFAULT_COUNT = 20;

  /** Service that reads timelines from the client abstraction. */
  private final TimelineService timelineService;

  /** Service that persists and publishes scheduled tweets. */
  private final SchedulingService schedulingService;

  /** Provider that resolves the caller's logical account id. */
  private final TokenProvider tokenProvider;

  /**
   * Creates a controller with required dependencies.
   *
   * @param timelineSvc timeline service
   * @param schedulingSvc scheduling service
   * @param tokenProv token provider
   */
  public ToolsController(final TimelineService timelineSvc,
                         final SchedulingService schedulingSvc,
                         final TokenProvider tokenProv) {
    this.timelineService = timelineSvc;
    this.schedulingService = schedulingSvc;
    this.tokenProvider = tokenProv;
  }

  /**
   * Returns a bounded number of tweets for the caller's home timeline.
   *
   * <p>Request body shape:
   * <pre>
   * {
   *   "tool": "get_home_timeline",
   *   "params": {"count": 2}
   * }
   * </pre>
   *
   * @param body request payload
   * @return JSON with an array field {@code tweets}
   */
  @PostMapping("/get_home_timeline")
  public ResponseEntity<?> getHomeTimeline(
      @RequestBody final Map<String, Object> body) {

    final Object paramsObj = body.getOrDefault("params", Map.of());
    @SuppressWarnings("unchecked")
    final Map<String, Object> params = (Map<String, Object>) paramsObj;

    final int count =
        params.get("count") instanceof Number
            ? ((Number) params.get("count")).intValue()
            : DEFAULT_COUNT;

    final String accountId = tokenProvider.accountIdForCaller();

    return ResponseEntity.ok(
        Map.of("tweets", timelineService.getHomeTimeline(accountId, count)));
  }

  /**
   * Schedules a tweet for a future time. If the provided time is in the past,
   * responds with HTTP 400.
   *
   * <p>Request body shape:
   * <pre>
   * {
   *   "tool": "schedule_tweet",
   *   "params": {"text": "...", "time": "2025-10-01T12:00:00Z"}
   * }
   * </pre>
   *
   * @param body request payload
   * @return JSON with {@code status}, {@code id}, and {@code scheduled_for}
   */
  @PostMapping("/schedule_tweet")
  public ResponseEntity<?> scheduleTweet(
      @RequestBody final Map<String, Object> body) {

    final Object paramsObj = body.getOrDefault("params", Map.of());
    @SuppressWarnings("unchecked")
    final Map<String, Object> params = (Map<String, Object>) paramsObj;

    final String text = String.valueOf(params.get("text"));
    final Instant runAt = Instant.parse(String.valueOf(params.get("time")));

    if (runAt.isBefore(Instant.now())) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of(
              "status", "error",
              "reason", "time must be in the future"));
    }

    final String accountId = tokenProvider.accountIdForCaller();
    final String id = schedulingService.schedule(text, runAt, accountId);

    return ResponseEntity.ok(
        Map.of(
            "status", "scheduled",
            "id", id,
            "text", text,
            "scheduled_for", runAt.toString()));
  }
}
