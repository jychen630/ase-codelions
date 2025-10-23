package com.team.mcp.search;

import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.TwitterClient.TwitterException;
import com.team.mcp.twitter.dto.Tweet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 * Search over tweets from either in-memory timeline (FakeTwitterClient)
 * or the DB {@code TWEETS} table, selected by {@code app.search.source}.
 *
 * <p>Modes:
 * <br>source=memory (default) → {@link TwitterClient}
 * <br>source=db → {@code SELECT * FROM TWEETS WHERE ACCOUNT_ID = ?}
 */
@Service
public final class SearchService {

  /** Default pool to scan from the timeline. */
  private static final int TIMELINE_POOL = 200;

  /** Default page size. */
  private static final int DEFAULT_LIMIT = 20;

  /** Hard upper bound for limit. */
  private static final int MAX_LIMIT = 100;

  /** Client used to fetch tweets (fake or real). */
  private final TwitterClient twitter;

  /** Optional JDBC handle for DB-backed search. */
  private final JdbcTemplate jdbc;

  /** Source selector: "memory" or "db". */
  private final String source;

  /**
   * Primary Spring constructor.
   *
   * @param twitterClient the Twitter client
   * @param jdbcTemplate JDBC template (may be {@code null} if not configured)
   * @param searchSource config flag: {@code memory} (default) or {@code db}
   */
  @Autowired
  public SearchService(
      final TwitterClient twitterClient,
      final JdbcTemplate jdbcTemplate,
      @Value("${app.search.source:memory}") final String searchSource) {
    this.twitter = Objects.requireNonNull(twitterClient, "twitter");
    this.jdbc = jdbcTemplate;
    this.source = searchSource == null || searchSource.isBlank()
        ? "memory"
        : searchSource;
  }

  /**
   * Backward-compatible constructor for unit tests using memory only.
   *
   * @param twitterClient the Twitter client
   */
  public SearchService(final TwitterClient twitterClient) {
    this(twitterClient, null, "memory");
  }

  /**
   * Search tweets with AND/OR and phrase support. Ranking by:
   * <ol>
   *   <li>token match score (phrases count double)</li>
   *   <li>recency (newer first)</li>
   * </ol>
   *
   * @param accountId account id
   * @param rawQuery raw query string
   * @param offset skip this many results (for pagination)
   * @param limit max results to return (capped)
   * @return a page of matching tweets (possibly empty, never {@code null})
   */
  public List<Tweet> search(
      final String accountId,
      final String rawQuery,
      final int offset,
      final int limit) {

    final int lim =
        Math.max(1, Math.min(limit <= 0 ? DEFAULT_LIMIT : limit, MAX_LIMIT));
    final int off = Math.max(0, offset);

    final SearchQuery parsed = SearchQuery.parse(rawQuery);
    final List<Tweet> pool = fetchPool(accountId, TIMELINE_POOL);

    final List<Scored> matches = new ArrayList<>();
    Instant newest = Instant.EPOCH;

    for (Tweet t : pool) {
      if (t.createdAt() != null && t.createdAt().isAfter(newest)) {
        newest = t.createdAt();
      }
    }

    for (Tweet t : pool) {
      final int score = scoreTweet(t, parsed);
      if (score > 0) {
        final long recency =
            newest.toEpochMilli()
                - (t.createdAt() == null ? 0L : t.createdAt().toEpochMilli());
        matches.add(new Scored(t, score, recency));
      }
    }

    matches.sort(
        Comparator.comparingInt(Scored::score)
            .reversed()
            .thenComparingLong(Scored::recency));

    final int from = Math.min(off, matches.size());
    final int to = Math.min(from + lim, matches.size());

    final List<Tweet> out = new ArrayList<>(Math.max(0, to - from));
    for (int i = from; i < to; i++) {
      out.add(matches.get(i).tweet());
    }
    return out;
  }

  /**
   * Hashtag search: exact case-insensitive match on a {@code #tag} token.
   *
   * @param accountId account id
   * @param hashtag hashtag including leading {@code '#'}
   * @param limit max results
   * @return matching tweets (possibly empty, never {@code null})
   */
  public List<Tweet> searchHashtag(
      final String accountId,
      final String hashtag,
      final int limit) {

    final String needle =
        hashtag == null ? "" : hashtag.trim().toLowerCase();
    if (!needle.startsWith("#")) {
      return List.of();
    }

    final int lim =
        Math.max(1, Math.min(limit <= 0 ? DEFAULT_LIMIT : limit, MAX_LIMIT));

    // Simple approach: reuse the same pool and filter.
    final List<Tweet> pool = fetchPool(accountId, TIMELINE_POOL);
    final List<Tweet> out = new ArrayList<>();

    for (Tweet t : pool) {
      final String txt = t.text() == null ? "" : t.text().toLowerCase();
      final String[] toks = txt.split("\\s+");
      for (String tok : toks) {
        if (needle.equals(tok)) {
          out.add(t);
          break;
        }
      }
      if (out.size() >= lim) {
        break;
      }
    }
    return out;
  }

  /**
   * Decide source and fetch a pool of tweets.
   *
   * @param accountId logical account id used for filtering
   * @param max maximum number of tweets to fetch
   * @return list of tweets (possibly empty, never {@code null})
   */
  private List<Tweet> fetchPool(final String accountId, final int max) {
    if ("db".equalsIgnoreCase(source) && jdbc != null) {
      return selectTweetsFromDb(accountId, max);
    }
    try {
      return twitter.getHomeTimeline(accountId, max);
    } catch (TwitterException ex) {
      return List.of();
    }
  }

  /**
   * Query the {@code TWEETS} table for an account.
   *
   * @param accountId account id to filter
   * @param max how many rows to return
   * @return list of Tweet DTOs ordered by {@code created_at DESC}
   */
  private List<Tweet> selectTweetsFromDb(
      final String accountId,
      final int max) {

    final String sql =
        "SELECT id, user_handle, text, created_at "
            + "FROM tweets "
            + "WHERE account_id = ? "
            + "ORDER BY created_at DESC "
            + "LIMIT ?";

    final RowMapper<Tweet> mapper = new RowMapper<>() {
      @Override
      public Tweet mapRow(final ResultSet rs, final int rowNum)
          throws SQLException {
        final String id = rs.getString("id");
        final String user = rs.getString("user_handle");
        final String text = rs.getString("text");
        final Instant created = rs.getTimestamp("created_at").toInstant();
        return new Tweet(id, user, text, created);
      }
    };

    return jdbc.query(sql, mapper, accountId, max);
  }

  /**
   * Compute a match score for a tweet against the parsed query.
   *
   * @param t tweet to check
   * @param q parsed search query
   * @return positive score if the tweet matches, otherwise {@code 0}
   */
  private static int scoreTweet(final Tweet t, final SearchQuery q) {
    final String txt = (t.text() == null) ? "" : t.text().toLowerCase();
    int best = 0;
    for (SearchQuery.Clause c : q.clauses()) {
      final int s = scoreClause(txt, c);
      if (s > best) {
        best = s;
      }
    }
    return best;
  }

  /**
   * Score a single AND-clause; phrases count double.
   *
   * @param txt normalized (lowercased) tweet text
   * @param c AND-clause containing terms and phrases
   * @return clause score; {@code 0} if any required term/phrase is missing
   */
  private static int scoreClause(
      final String txt,
      final SearchQuery.Clause c) {

    int termHits = 0;
    for (String term : c.terms()) {
      if (!txt.contains(term)) {
        return 0;
      }
      termHits++;
    }

    int phraseHits = 0;
    for (String ph : c.phrases()) {
      if (!txt.contains(ph)) {
        return 0;
      }
      phraseHits++;
    }

    return termHits + (phraseHits * 2);
  }

  /** Small container for sorting matched tweets. */
  private static final class Scored {

    /** The tweet that matched. */
    private final Tweet tweet;

    /** Match score (higher is better). */
    private final int score;

    /** Recency key (lower is newer). */
    private final long recency;

    Scored(final Tweet t, final int s, final long r) {
      this.tweet = t;
      this.score = s;
      this.recency = r;
    }

    Tweet tweet() {
      return tweet;
    }

    int score() {
      return score;
    }

    long recency() {
      return recency;
    }
  }
}
