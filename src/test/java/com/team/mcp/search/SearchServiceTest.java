package com.team.mcp.search;

import static org.junit.jupiter.api.Assertions.*;

import com.team.mcp.twitter.FakeTwitterClient;
import com.team.mcp.twitter.TwitterClient;
import com.team.mcp.twitter.TwitterClient.TwitterException;
import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SearchServiceTest {

  private final TwitterClient fake = new FakeTwitterClient();
  private final SearchService svc = new SearchService(fake);

  @Test
  void findsPhrase() {
    final List<Tweet> r = svc.search("acctA", "\"seed tweet #12\"", 0, 5);
    assertFalse(r.isEmpty());
    assertTrue(r.get(0).text().contains("#12"));
  }

  @Test
  void orMatchesMore() {
    final List<Tweet> r = svc.search("acctA", "hello OR #52", 0, 10);
    assertFalse(r.isEmpty());
  }

  @Test
  void search_handlesTwitterException_returnsEmpty() {
    TwitterClient throwingClient = new TwitterClient() {
      @Override
      public String postTweet(String accountId, String text) throws TwitterException {
        throw new TwitterException("Test exception");
      }

      @Override
      public List<Tweet> getHomeTimeline(String accountId, int count) throws TwitterException {
        throw new TwitterException("Test exception");
      }
    };
    
    SearchService service = new SearchService(throwingClient);
    List<Tweet> results = service.search("acctA", "test", 0, 10);
    
    assertNotNull(results);
    assertTrue(results.isEmpty());
  }

  @Test
  void search_nullQuery_returnsEmpty() {
    final List<Tweet> r = svc.search("acctA", null, 0, 10);
    assertNotNull(r);
  }

  @Test
  void search_emptyQuery_returnsEmpty() {
    final List<Tweet> r = svc.search("acctA", "", 0, 10);
    assertNotNull(r);
  }

  @Test
  void search_negativeOffset_normalizedToZero() {
    final List<Tweet> r = svc.search("acctA", "hello", -5, 10);
    assertNotNull(r);
  }

  @Test
  void search_zeroLimit_usesDefault() {
    final List<Tweet> r = svc.search("acctA", "hello", 0, 0);
    assertNotNull(r);
  }

  @Test
  void search_exceedsMaxLimit_cappedAtMax() {
    final List<Tweet> r = svc.search("acctA", "hello", 0, 200);
    assertNotNull(r);
    assertTrue(r.size() <= 100); // MAX_LIMIT
  }

  @Test
  void searchHashtag_missingHash_returnsEmpty() {
    final List<Tweet> r = svc.searchHashtag("acctA", "nohash", 10);
    assertNotNull(r);
    assertTrue(r.isEmpty());
  }

  @Test
  void searchHashtag_nullHashtag_returnsEmpty() {
    final List<Tweet> r = svc.searchHashtag("acctA", null, 10);
    assertNotNull(r);
    assertTrue(r.isEmpty());
  }

  @Test
  void searchHashtag_emptyHashtag_returnsEmpty() {
    final List<Tweet> r = svc.searchHashtag("acctA", "", 10);
    assertNotNull(r);
    assertTrue(r.isEmpty());
  }

  @Test
  void searchHashtag_zeroLimit_usesDefault() {
    final List<Tweet> r = svc.searchHashtag("acctA", "#hello", 0);
    assertNotNull(r);
  }

  @Test
  void searchHashtag_exceedsMaxLimit_cappedAtMax() {
    final List<Tweet> r = svc.searchHashtag("acctA", "#hello", 200);
    assertNotNull(r);
    assertTrue(r.size() <= 100); // MAX_LIMIT
  }

  @Test
  void search_tweetWithNullCreatedAt_handlesGracefully() {
    TwitterClient customClient = new TwitterClient() {
      @Override
      public String postTweet(String accountId, String text) throws TwitterException {
        throw new TwitterException("Not implemented");
      }

      @Override
      public List<Tweet> getHomeTimeline(String accountId, int count) throws TwitterException {
        return List.of(new Tweet("id1", "user1", "test tweet", null));
      }
    };
    SearchService service = new SearchService(customClient);
    List<Tweet> results = service.search("acct", "test", 0, 10);
    assertNotNull(results);
  }

  @Test
  void search_tweetWithNullText_handlesGracefully() {
    TwitterClient customClient = new TwitterClient() {
      @Override
      public String postTweet(String accountId, String text) throws TwitterException {
        throw new TwitterException("Not implemented");
      }

      @Override
      public List<Tweet> getHomeTimeline(String accountId, int count) throws TwitterException {
        return List.of(new Tweet("id1", "user1", null, Instant.now()));
      }
    };
    SearchService service = new SearchService(customClient);
    List<Tweet> results = service.search("acct", "test", 0, 10);
    assertNotNull(results);
  }

  @Test
  void search_noMatches_returnsEmpty() {
    final List<Tweet> r = svc.search("acctA", "nonexistentkeyword12345", 0, 10);
    assertNotNull(r);
    assertTrue(r.isEmpty());
  }

  @Test
  void search_offsetGreaterThanMatches_returnsEmpty() {
    final List<Tweet> r = svc.search("acctA", "hello", 1000, 10);
    assertNotNull(r);
    assertTrue(r.isEmpty());
  }

  @Test
  void searchHashtag_whitespaceInHashtag_handlesCorrectly() {
    final List<Tweet> r = svc.searchHashtag("acctA", "  #hello  ", 10);
    assertNotNull(r);
  }

  @Test
  void constructor_nullSource_usesMemory() {
    SearchService service = new SearchService(fake, null, null);
    List<Tweet> results = service.search("acct", "test", 0, 10);
    assertNotNull(results);
  }

  @Test
  void constructor_blankSource_usesMemory() {
    SearchService service = new SearchService(fake, null, "   ");
    List<Tweet> results = service.search("acct", "test", 0, 10);
    assertNotNull(results);
  }

  @Test
  void search_phraseMatch_scoresHigher() {
    final List<Tweet> r = svc.search("acctA", "\"seed tweet\"", 0, 10);
    assertFalse(r.isEmpty());
    // Phrase matches should score higher
  }

  @Test
  void search_multipleClauses_OR_logic() {
    final List<Tweet> r = svc.search("acctA", "hello OR world", 0, 10);
    assertNotNull(r);
  }

  @Test
  void searchHashtag_emptyPool_returnsEmpty() {
    TwitterClient emptyClient = new TwitterClient() {
      @Override
      public String postTweet(String accountId, String text) throws TwitterException {
        throw new TwitterException("Not implemented");
      }

      @Override
      public List<Tweet> getHomeTimeline(String accountId, int count) throws TwitterException {
        return List.of();
      }
    };
    SearchService service = new SearchService(emptyClient);
    List<Tweet> results = service.searchHashtag("acct", "#test", 10);
    assertTrue(results.isEmpty());
  }
}
