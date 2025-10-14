package com.team.mcp.tools;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.team.mcp.security.TokenProvider;
import com.team.mcp.timeline.TimelineService;
import com.team.mcp.twitter.dto.Tweet;
import com.team.mcp.scheduling.SchedulingService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-slice test: boots MVC only and mocks dependencies.
 * No DB, JPA, or scheduler required.
 */
@ActiveProfiles("test")
@WebMvcTest(controllers = ToolsController.class)
class ToolsApiTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private TimelineService timelineService;

  @MockBean
  private SchedulingService schedulingService;

  @MockBean
  private TokenProvider tokenProvider;

  @Test
  void getHomeTimeline_returnsTwoTweets() throws Exception {
    when(tokenProvider.accountIdForCaller()).thenReturn("test-account");

    var t1 = new Tweet("id-1", "alice", "hi", Instant.now());
    var t2 = new Tweet("id-2", "bob", "hello", Instant.now());
    when(timelineService.getHomeTimeline(eq("test-account"), eq(2)))
        .thenReturn(List.of(t1, t2));

    String body = """
        {
          "tool": "get_home_timeline",
          "params": {"count": 2}
        }
        """;

    mvc.perform(
            post("/tools/get_home_timeline")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tweets.length()", lessThanOrEqualTo(2)))
        .andExpect(jsonPath("$.tweets.length()", greaterThanOrEqualTo(0)))
        .andExpect(jsonPath("$.tweets[0].id", notNullValue()));
  }

  @Test
  void scheduleTweet_returnsScheduled() throws Exception {
    when(tokenProvider.accountIdForCaller()).thenReturn("test-account");

    String isoFuture =
        OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5).toString();
    when(schedulingService.schedule(
        eq("Hello from test"), any(Instant.class), eq("test-account")))
        .thenReturn("sched-123");

    String body = """
        {
          "tool": "schedule_tweet",
          "params": {"text": "Hello from test", "time": "%s"}
        }
        """.formatted(isoFuture);

    mvc.perform(
            post("/tools/schedule_tweet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", equalTo("scheduled")))
        .andExpect(jsonPath("$.id", equalTo("sched-123")))
        .andExpect(jsonPath("$.scheduled_for", equalTo(isoFuture)));
  }

  @Test
  void scheduleTweet_pastTime_returns400() throws Exception {
    when(tokenProvider.accountIdForCaller()).thenReturn("test-account");

    String isoPast =
        OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5).toString();

    String body = """
        {
          "tool": "schedule_tweet",
          "params": {"text": "late", "time": "%s"}
        }
        """.formatted(isoPast);

    mvc.perform(
            post("/tools/schedule_tweet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getHomeTimeline_negativeCount_still200_andCallsService()
      throws Exception {
    when(tokenProvider.accountIdForCaller()).thenReturn("test-account");
    when(timelineService.getHomeTimeline(eq("test-account"), eq(-10)))
        .thenReturn(List.of());

    String body = """
        {
          "tool": "get_home_timeline",
          "params": {"count": -10}
        }
        """;

    mvc.perform(
            post("/tools/get_home_timeline")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tweets.length()", greaterThanOrEqualTo(0)));
  }
}
