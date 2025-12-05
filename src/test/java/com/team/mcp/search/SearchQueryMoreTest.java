package com.team.mcp.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Extra coverage for SearchQuery parsing edge cases.
 */
final class SearchQueryMoreTest {

  @Test
  void parseNullOrBlank_yieldsSingleEmptyClause() {
    var q1 = SearchQuery.parse(null);
    assertEquals(1, q1.clauses().size());
    assertTrue(q1.clauses().get(0).terms().isEmpty());
    assertTrue(q1.clauses().get(0).phrases().isEmpty());

    var q2 = SearchQuery.parse("   ");
    assertEquals(1, q2.clauses().size());
    assertTrue(q2.clauses().get(0).terms().isEmpty());
    assertTrue(q2.clauses().get(0).phrases().isEmpty());
  }

  @Test
  void parseCaseInsensitiveOrAndPhrases() {
    var q = SearchQuery.parse("hello or world OR \"seed tweet\"");
    assertEquals(3, q.clauses().size());
    assertEquals(1, q.clauses().get(0).terms().size());
    assertEquals(1, q.clauses().get(1).terms().size());
    assertEquals(0, q.clauses().get(2).terms().size());
    assertEquals(1, q.clauses().get(2).phrases().size());
  }

  @Test
  void parseSinglePhraseOnly() {
    var q = SearchQuery.parse("\"hello world\"");
    assertEquals(1, q.clauses().size());
    assertEquals(0, q.clauses().get(0).terms().size());
    assertEquals(1, q.clauses().get(0).phrases().size());
  }
}

