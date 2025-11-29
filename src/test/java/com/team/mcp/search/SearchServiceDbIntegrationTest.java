package com.team.mcp.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.team.mcp.twitter.FakeTwitterClient;
import com.team.mcp.twitter.dto.Tweet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link SearchService} in "db" mode using real JDBC.
 *
 * <p>Populates the {@code tweets} table via {@link JdbcTemplate} and
 * verifies that {@link SearchService} retrieves matches from the DB.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
class SearchServiceDbIntegrationTest {

  @Autowired
  private JdbcTemplate jdbc;

  private SearchService svc;

  @BeforeEach
  void setUp() {
    // Use "db" mode so SearchService queries the database via JdbcTemplate.
    svc = new SearchService(new FakeTwitterClient(), jdbc, "db");

    // Clean table before each test
    jdbc.update("DELETE FROM tweets");

    Instant base = Instant.parse("2025-01-01T00:00:00Z");

    // Note: test schema does NOT have account_id, so we only insert the existing columns.
    jdbc.update(
        "INSERT INTO tweets (id, user_handle, text, created_at) VALUES (?,?,?,?)",
        "db1", "user1", "hello from db search", base);

    jdbc.update(
        "INSERT INTO tweets (id, user_handle, text, created_at) VALUES (?,?,?,?)",
        "db2", "user2", "other content", base.plusSeconds(1));
  }

  @Test
  void search_inDbMode_readsFromDatabase() {
    // accountId is effectively ignored in "db" mode because the table has no account_id column
    List<Tweet> results = svc.search("ignoredAccount", "hello", 0, 10);

    assertFalse(results.isEmpty());
    assertEquals(1, results.size());
    assertEquals("db1", results.get(0).id());
  }
}
