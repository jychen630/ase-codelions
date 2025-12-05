package com.team.mcp.search;

import static org.junit.jupiter.api.Assertions.*;

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

  @Test
  void parse_nullQuery_returnsEmptyClause() {
    final SearchQuery q = SearchQuery.parse(null);
    assertNotNull(q);
    assertEquals(1, q.clauses().size());
    assertTrue(q.clauses().get(0).terms().isEmpty());
    assertTrue(q.clauses().get(0).phrases().isEmpty());
  }

  @Test
  void parse_emptyQuery_returnsEmptyClause() {
    final SearchQuery q = SearchQuery.parse("");
    assertNotNull(q);
    assertEquals(1, q.clauses().size());
  }

  @Test
  void parse_blankQuery_returnsEmptyClause() {
    final SearchQuery q = SearchQuery.parse("   ");
    assertNotNull(q);
    assertEquals(1, q.clauses().size());
  }

  @Test
  void parse_unclosedQuote_handlesGracefully() {
    final SearchQuery q = SearchQuery.parse("\"unclosed quote");
    assertNotNull(q);
    // Unclosed quote should still parse what it can
  }

  @Test
  void parse_emptyQuotes_ignored() {
    final SearchQuery q = SearchQuery.parse("\"\"");
    assertNotNull(q);
    assertEquals(1, q.clauses().size());
    assertTrue(q.clauses().get(0).phrases().isEmpty());
  }

  @Test
  void parse_multipleOR_splitsCorrectly() {
    final SearchQuery q = SearchQuery.parse("a OR b OR c");
    assertEquals(3, q.clauses().size());
  }

  @Test
  void parse_caseInsensitiveOR() {
    final SearchQuery q = SearchQuery.parse("a or b Or c");
    assertEquals(3, q.clauses().size());
  }

  @Test
  void parse_termsAndPhrasesTogether() {
    final SearchQuery q = SearchQuery.parse("hello \"world\" test");
    assertEquals(1, q.clauses().size());
    assertEquals(2, q.clauses().get(0).terms().size());
    assertEquals(1, q.clauses().get(0).phrases().size());
  }

  @Test
  void parse_onlyOR_returnsEmptyClauses() {
    final SearchQuery q = SearchQuery.parse("OR OR");
    assertNotNull(q);
    // Should handle gracefully
  }
}
