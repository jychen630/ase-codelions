package com.team.mcp.search;

import com.team.mcp.twitter.dto.Tweet;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP endpoints for tweet search and discovery.
 */
@RestController
public final class SearchController {

  /** Application search service (memory or DB-backed depending on config). */
  private final SearchService svc;

  /**
   * Constructs the controller.
   *
   * @param service injected {@link SearchService}
   */
  public SearchController(final SearchService service) {
    this.svc = service;
  }

  /**
   * Keyword/phrase search with AND/OR and pagination.
   *
   * <p>Examples:</p>
   * <pre>
   *   /search?accountId=acctA&q="seed tweet" OR hello&offset=0&limit=10
   * </pre>
   *
   * @param accountId logical account id
   * @param q raw query string (supports phrases in quotes and {@code OR})
   * @param offset number of results to skip (for pagination)
   * @param limit maximum number of results to return
   * @return HTTP 200 with a list of matching {@link Tweet} DTOs
   */
  @GetMapping("/search")
  public ResponseEntity<List<Tweet>> search(
      @RequestParam("accountId") final String accountId,
      @RequestParam("q") final String q,
      @RequestParam(value = "offset", defaultValue = "0") final int offset,
      @RequestParam(value = "limit", defaultValue = "20") final int limit) {

    final List<Tweet> out = svc.search(accountId, q, offset, limit);
    return ResponseEntity.ok(out);
  }

  /**
   * Exact hashtag search (case-insensitive). Query must start with '#'.
   *
   * @param accountId logical account id
   * @param q hashtag to search (must begin with '#')
   * @param limit maximum number of results to return
   * @return HTTP 400 with error map if {@code q} does not start with '#';
   *         otherwise HTTP 200 with a list of matching {@link Tweet} DTOs
   */
  @GetMapping("/search/hashtags")
  public ResponseEntity<?> hashtags(
      @RequestParam("accountId") final String accountId,
      @RequestParam("q") final String q,
      @RequestParam(value = "limit", defaultValue = "20") final int limit) {

    if (q == null || !q.startsWith("#")) {
      return ResponseEntity.badRequest().body(
          java.util.Map.of("error", "query must start with '#'"));
    }
    final List<Tweet> out = svc.searchHashtag(accountId, q, limit);
    return ResponseEntity.ok(out);
  }
}
