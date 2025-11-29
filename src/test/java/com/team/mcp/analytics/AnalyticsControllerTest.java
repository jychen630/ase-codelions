package com.team.mcp.analytics;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MVC tests for {@link AnalyticsController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
final class AnalyticsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AnalyticsService service;

  @Test
  void topHashtags_endpointReturnsList() throws Exception {
    when(service.topHashtags("acct", 5))
        .thenReturn(List.of("#a", "#b"));

    mockMvc.perform(get("/analytics/top-hashtags")
            .param("accountId", "acct"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0]").value("#a"))
        .andExpect(jsonPath("$[1]").value("#b"));
  }

  @Test
  void bestHours_endpointReturnsMap() throws Exception {
    when(service.bestHours("acct"))
        .thenReturn(Map.of(0, 2, 1, 3));

    mockMvc.perform(get("/analytics/best-hours")
            .param("accountId", "acct"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        // Map<Integer, Integer> is serialized with string keys "0", "1"
        .andExpect(jsonPath("$['0']").value(2))
        .andExpect(jsonPath("$['1']").value(3));
  }

  @Test
  void summary_endpointReturnsSummary() throws Exception {
    AnalyticsService.Summary summary =
        new AnalyticsService.Summary(10, List.of("#x"), 3);
    when(service.summary("acct")).thenReturn(summary);

    mockMvc.perform(get("/analytics/summary")
            .param("accountId", "acct"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalTweets").value(10))
        .andExpect(jsonPath("$.topHashtags[0]").value("#x"))
        .andExpect(jsonPath("$.bestHourUtc").value(3));
  }

  @Test
  void sentiment_endpointReturnsSummary() throws Exception {
    AnalyticsService.SentimentSummary sentiment =
        new AnalyticsService.SentimentSummary(3, 1, 1, 1, 0.0);
    when(service.sentimentSummary("acct")).thenReturn(sentiment);

    mockMvc.perform(get("/analytics/sentiment")
            .param("accountId", "acct"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalTweets").value(3))
        .andExpect(jsonPath("$.positive").value(1))
        .andExpect(jsonPath("$.negative").value(1))
        .andExpect(jsonPath("$.neutral").value(1))
        .andExpect(jsonPath("$.averageScore").value(0.0));
  }
}

