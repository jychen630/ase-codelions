package com.team.mcp.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed search query supporting the following.
 * <ul>
 *   <li><b>AND</b> within a clause (tokens + phrases)</li>
 *   <li><b>OR</b> across clauses (by the literal token {@code "OR"})</li>
 *   <li>Phrases inside double quotes</li>
 * </ul>
 *
 * <p>Examples:
 * <pre>
 *   hello world            -> one clause: ["hello","world"]
 *   hello OR "seed tweet"  -> two clauses
 * </pre>
 */
public final class SearchQuery {

  /**
   * A single OR-clause: all tokens/phrases inside the clause must match
   * (logical AND).
   */
  public static final class Clause {

    /** Lowercased AND-terms that must all appear for a match. */
    private final List<String> terms = new ArrayList<>();

    /** Lowercased quoted phrases that must all appear for a match. */
    private final List<String> phrases = new ArrayList<>();

    /**
     * Adds a single term to this clause (ignored if null/blank).
     *
     * @param t term to add; will be normalized to lower case
     */
    void addTerm(final String t) {
      if (t != null && !t.isBlank()) {
        terms.add(t.toLowerCase());
      }
    }

    /**
     * Adds a quoted phrase to this clause (ignored if null/blank).
     *
     * @param p phrase to add; will be normalized to lower case
     */
    void addPhrase(final String p) {
      if (p != null && !p.isBlank()) {
        phrases.add(p.toLowerCase());
      }
    }

    /**
     * Returns the list of lowercased AND-terms.
     *
     * @return list of terms (never null)
     */
    public List<String> terms() {
      return terms;
    }

    /**
     * Returns the list of lowercased quoted phrases.
     *
     * @return list of phrases (never null)
     */
    public List<String> phrases() {
      return phrases;
    }
  }

  /** Ordered list of OR-clauses that make up the whole query. */
  private final List<Clause> clauses;

  /**
   * Constructs a query from already-parsed clauses.
   *
   * @param cs the OR-clauses composing this query
   */
  private SearchQuery(final List<Clause> cs) {
    this.clauses = cs;
  }

  /**
   * Returns the list of OR-clauses (AND inside each clause).
   *
   * @return list of clauses (never null)
   */
  public List<Clause> clauses() {
    return clauses;
  }

  /**
   * Parses a human query into OR-clauses and AND-tokens.
   *
   * <p>Splits on the token {@code OR} (case-insensitive), extracts quoted
   * phrases, and treats remaining whitespace-separated tokens as AND-terms.
   *
   * @param raw raw query string (may be null/blank)
   * @return a {@link SearchQuery} representing the parsed structure
   */
  public static SearchQuery parse(final String raw) {
    final String q = raw == null ? "" : raw.trim();
    final List<String> parts = splitByOr(q);
    final List<Clause> out = new ArrayList<>();
    for (String p : parts) {
      out.add(parseClause(p));
    }
    return new SearchQuery(out);
  }

  /**
   * Splits a string by {@code OR} when used as a separate token
   * (case-insensitive).
   *
   * @param s input string (never mutated)
   * @return non-empty list of segments; if no content, a single empty segment
   */
  private static List<String> splitByOr(final String s) {
    // Simple split on OR as a separate token (case-insensitive).
    final List<String> out = new ArrayList<>();
    final String[] tokens = s.split("(?i)\\s+OR\\s+");
    for (String t : tokens) {
      final String x = t.trim();
      if (!x.isBlank()) {
        out.add(x);
      }
    }
    if (out.isEmpty()) {
      out.add("");
    }
    return out;
  }

  /**
   * Parses a single clause into its phrases and remaining terms.
   *
   * @param s clause text
   * @return a populated {@link Clause}
   */
  private static Clause parseClause(final String s) {
    final Clause c = new Clause();

    // Extract phrases "like this"
    final StringBuilder sb = new StringBuilder();
    boolean inQuote = false;
    final List<String> phrases = new ArrayList<>();
    for (int i = 0; i < s.length(); i++) {
      final char ch = s.charAt(i);
      if (ch == '"') {
        inQuote = !inQuote;
        if (!inQuote) {
          final String phrase = sb.toString().trim();
          if (!phrase.isEmpty()) {
            phrases.add(phrase);
          }
          sb.setLength(0);
        }
      } else if (inQuote) {
        sb.append(ch);
      }
    }
    for (String ph : phrases) {
      c.addPhrase(ph);
    }

    // Remove phrases from the clause string; split remaining by spaces.
    final String noPhrases = s.replaceAll("\"[^\"]*\"", " ").trim();
    for (String term : noPhrases.split("\\s+")) {
      if (!term.isBlank()) {
        c.addTerm(term);
      }
    }
    return c;
  }
}
