package com.team.mcp.tools;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.team.mcp.scheduling.SchedulingService;
import com.team.mcp.security.TokenProvider;
import com.team.mcp.timeline.TimelineService;
import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for ToolsController that don't require Spring context.
 * This avoids Mockito/Java 25 compatibility issues with @MockBean.
 */
class ToolsControllerUnitTest {

  private TimelineService timelineService;
  private SchedulingService schedulingService;
  private TokenProvider tokenProvider;
  private ToolsController controller;

  @BeforeEach
  void setUp() {
    timelineService = mock(TimelineService.class);
    schedulingService = mock(SchedulingService.class);
    tokenProvider = mock(TokenProvider.class);
    controller = new ToolsController(timelineService, schedulingService, tokenProvider);
  }

  @Test
  void getHomeTimeline_withValidCount_returnsTweets() {
    when(tokenProvider.accountIdForCaller()).thenReturn("test-account");
    var t1 = new Tweet("id-1", "alice", "hi", Instant.now());
    var t2 = new Tweet("id-2", "bob", "hello", Instant.now());
    when(timelineService.getHomeTimeline("test-account", 2))
        .thenReturn(List.of(t1, t2));

    Map<String, Object> body = Map.of(
        "tool", "get_home_timeline",
        "params", Map.of("count", 2)
    );

    ResponseEntity<?> response = controller.getHomeTimeline(body);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
    assertNotNull(responseBody);
    @SuppressWarnings("unchecked")
    List<Tweet> tweets = (List<Tweet>) responseBody.get("tweets");
    assertEquals(2, tweets.size());
  }

  @Test
  void getHomeTimeline_missingParams_usesDefaultCount() {
    when(tokenProvider.accountIdForCaller()).thenReturn("test-account");
    when(timelineService.getHomeTimeline("test-account", 20))
        .thenReturn(List.of());

    Map<String, Object> body = Map.of("tool", "get_home_timeline");

    ResponseEntity<?> response = controller.getHomeTimeline(body);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(timelineService).getHomeTimeline("test-account", 20);
  }

  @Test
  void getHomeTimeline_nonNumberCount_usesDefaultCount() {
    when(tokenProvider.accountIdForCaller()).thenReturn("test-account");
    when(timelineService.getHomeTimeline("test-account", 20))
        .thenReturn(List.of());

    Map<String, Object> body = Map.of(
        "tool", "get_home_timeline",
        "params", Map.of("count", "not-a-number")
    );

    ResponseEntity<?> response = controller.getHomeTimeline(body);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(timelineService).getHomeTimeline("test-account", 20);
  }

  @Test
  void getHomeTimeline_negativeCount_callsServiceWithNegative() {
    when(tokenProvider.accountIdForCaller()).thenReturn("test-account");
    when(timelineService.getHomeTimeline("test-account", -10))
        .thenReturn(List.of());

    Map<String, Object> body = Map.of(
        "tool", "get_home_timeline",
        "params", Map.of("count", -10)
    );

    ResponseEntity<?> response = controller.getHomeTimeline(body);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(timelineService).getHomeTimeline("test-account", -10);
  }

  @Test
  void scheduleTweet_withFutureTime_returnsScheduled() {
    when(tokenProvider.accountIdForCaller()).thenReturn("test-account");
    String isoFuture = OffsetDateTime.now(ZoneOffset.UTC)
        .plusMinutes(5).toString();
    when(schedulingService.schedule(
        eq("Hello from test"), any(Instant.class), eq("test-account")))
        .thenReturn("sched-123");

    Map<String, Object> body = Map.of(
        "tool", "schedule_tweet",
        "params", Map.of(
            "text", "Hello from test",
            "time", isoFuture
        )
    );

    ResponseEntity<?> response = controller.scheduleTweet(body);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
    assertNotNull(responseBody);
    assertEquals("scheduled", responseBody.get("status"));
    assertEquals("sched-123", responseBody.get("id"));
  }

  @Test
  void scheduleTweet_withPastTime_returns400() {
    when(tokenProvider.accountIdForCaller()).thenReturn("test-account");
    String isoPast = OffsetDateTime.now(ZoneOffset.UTC)
        .minusMinutes(5).toString();

    Map<String, Object> body = Map.of(
        "tool", "schedule_tweet",
        "params", Map.of(
            "text", "late",
            "time", isoPast
        )
    );

    ResponseEntity<?> response = controller.scheduleTweet(body);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
    assertNotNull(responseBody);
    assertEquals("error", responseBody.get("status"));
    assertTrue(responseBody.get("reason").toString().contains("future"));
  }

  @Test
  void scheduleTweet_withCurrentTime_returns400() {
    when(tokenProvider.accountIdForCaller()).thenReturn("test-account");
    String currentTime = Instant.now().toString();

    Map<String, Object> body = Map.of(
        "tool", "schedule_tweet",
        "params", Map.of(
            "text", "Hello",
            "time", currentTime
        )
    );

    ResponseEntity<?> response = controller.scheduleTweet(body);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void scheduleTweet_missingParams_throwsException() {
    when(tokenProvider.accountIdForCaller()).thenReturn("test-account");

    Map<String, Object> body = Map.of("tool", "schedule_tweet");

    // This should throw an exception when trying to parse null time
    assertThrows(Exception.class, () -> {
      controller.scheduleTweet(body);
    });
  }

  @Test
  void scheduleTweet_invalidTimeFormat_throwsException() {
    when(tokenProvider.accountIdForCaller()).thenReturn("test-account");

    Map<String, Object> body = Map.of(
        "tool", "schedule_tweet",
        "params", Map.of(
            "text", "Hello",
            "time", "invalid-date"
        )
    );

    // This should throw an exception when trying to parse invalid date
    assertThrows(Exception.class, () -> {
      controller.scheduleTweet(body);
    });
  }
}

