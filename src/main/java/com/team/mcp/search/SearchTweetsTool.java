package com.team.mcp.mcp;

import com.team.mcp.search.SearchService;
import com.team.mcp.twitter.dto.Tweet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * MCP tool to search tweets from the home timeline.
 *
 * <p>Args:
 * <ul>
 *   <li><b>accountId</b> (required)</li>
 *   <li><b>q</b> (required) — supports AND / OR / quoted phrases</li>
 *   <li><b>offset</b> (optional)</li>
 *   <li><b>limit</b> (optional)</li>
 * </ul>
 */
@Component
public final class SearchTweetsTool implements Tool {

  /** Service that executes keyword/phrase/hashtag searches. */
  private final SearchService search;

  /**
   * Creates the tool.
   *
   * @param searchService backing search service used to query tweets
   */
  public SearchTweetsTool(final SearchService searchService) {
    this.search = searchService;
  }

  @Override
  public String name() {
    return "search_tweets";
  }

  @Override
  public String description() {
    return "Search timeline tweets with AND/OR and phrase support";
  }

  @Override
  public List<Map<String, Object>> call(final Map<String, Object> args) {
    final Object acc = args.get("accountId");
    final Object q = args.get("q");
    if (!(acc instanceof String) || ((String) acc).isBlank()
        || !(q instanceof String) || ((String) q).isBlank()) {
      return List.of(Map.of(
          "type", "text",
          "text", "error: 'accountId' and 'q' are required"));
    }

    final int offset = getInt(args.get("offset"), 0);
    final int limit = getInt(args.get("limit"), 20);

    final List<Tweet> results =
        search.search((String) acc, (String) q, offset, limit);

    final List<Map<String, Object>> content = new ArrayList<>();
    if (results.isEmpty()) {
      content.add(Map.of("type", "text", "text", "no matches"));
      return content;
    }

    // Return compact text rows to keep it simple.
    final StringBuilder sb = new StringBuilder();
    for (Tweet t : results) {
      sb.append(t.id()).append(" | ")
          .append(t.user()).append(" | ")
          .append(t.text()).append("\n");
    }
    content.add(Map.of("type", "text", "text", sb.toString().trim()));
    return content;
  }

  /**
   * Parse an integer argument that may be Number or String.
   *
   * @param o raw value
   * @param def default to use if parsing fails or value is null
   * @return parsed integer or {@code def}
   */
  private static int getInt(final Object o, final int def) {
    if (o instanceof Number) {
      return ((Number) o).intValue();
    }
    if (o instanceof String) {
      try {
        return Integer.parseInt((String) o);
      } catch (NumberFormatException e) {
        return def;
      }
    }
    return def;
  }
}
