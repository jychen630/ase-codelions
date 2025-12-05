package com.team.mcp.search;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice tests for SearchController.
 */
@ActiveProfiles("test")
@WebMvcTest(controllers = SearchController.class)
class SearchControllerTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private SearchService searchService;

  @Test
  void search_returnsTweetsFromService() throws Exception {
    var t1 = new Tweet("id-1", "alice", "hello world", Instant.now());
    var t2 = new Tweet("id-2", "bob", "hi there", Instant.now());

    when(searchService.search(eq("acctA"), eq("hello"), eq(0), eq(10), any(SearchFilters.class)))
        .thenReturn(List.of(t1, t2));

    mvc.perform(
            get("/search")
                .param("accountId", "acctA")
                .param("q", "hello")
                .param("offset", "0")
                .param("limit", "10")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id", equalTo("id-1")))
        .andExpect(jsonPath("$[0].user", equalTo("alice")));
  }

  @Test
  void hashtags_validQuery_returnsFromService() throws Exception {
    var t = new Tweet("id-3", "charlie", "#tag stuff", Instant.now());
    when(searchService.searchHashtag("acctB", "#tag", 5))
        .thenReturn(List.of(t));

    mvc.perform(
            get("/search/hashtags")
                .param("accountId", "acctB")
                .param("q", "#tag")
                .param("limit", "5")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id", equalTo("id-3")))
        .andExpect(jsonPath("$[0].user", equalTo("charlie")));
  }

  @Test
  void hashtags_missingHash_returns400WithErrorBody() throws Exception {
    // service should not be called in this case; we only care about controller behavior
    when(searchService.searchHashtag(anyString(), anyString(), anyInt()))
        .thenReturn(List.of());

    mvc.perform(
            get("/search/hashtags")
                .param("accountId", "acctC")
                .param("q", "not-a-hash")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", equalTo("query must start with '#'")));
  }
}

