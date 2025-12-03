package com.team.mcp.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class SearchQueryTest {

  @Test
  void parseAndOrPhrase() {
    final SearchQuery q = SearchQuery.parse("\"seed tweet\" OR hello world");
    assertEquals(2, q.clauses().size());
    assertEquals(0, q.clauses().get(0).terms().size());
    assertEquals(1, q.clauses().get(0).phrases().size());
    assertEquals(2, q.clauses().get(1).terms().size());
  }
}
