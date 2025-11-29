package com.team.mcp.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuditServiceTest {

  @Autowired
  private ToolCallAuditRepository repo;

  @Test
  void save_persistsRow_andTruncatesLongError() {
    AuditService svc = new AuditService(repo);

    String longMsg = "x".repeat(ToolCallAudit.LEN_ERROR + 10);
    svc.save("tools/call", "search_tweets",
        "acctA", false, 123L, 400, longMsg);

    List<ToolCallAudit> all = repo.findAll();
    assertEquals(1, all.size());
    ToolCallAudit row = all.get(0);

    assertNotNull(row.getId());

    String rpcMethod = (String)
        ReflectionTestUtils.getField(row, "rpcMethod");
    String toolName = (String)
        ReflectionTestUtils.getField(row, "toolName");
    Integer code = (Integer)
        ReflectionTestUtils.getField(row, "errorCode");
    String errMsg = (String)
        ReflectionTestUtils.getField(row, "errorMessage");
    Object createdAt = ReflectionTestUtils.getField(row, "createdAt");

    assertEquals("tools/call", rpcMethod);
    assertEquals("search_tweets", toolName);
    assertEquals(400, code);
    assertNotNull(createdAt);
    assertNotNull(errMsg);
    assertEquals(ToolCallAudit.LEN_ERROR, errMsg.length(),
        "should be truncated");
  }

  @Test
  void save_allowsNullErrorMessage_andShortMessages() {
    AuditService svc = new AuditService(repo);

    // null message -> stays null
    svc.save("m1", "t1", "a1", true, 10L, null, null);

    // short message -> not truncated
    String shortMsg = "short";
    svc.save("m2", "t2", "a2", false, 20L, 500, shortMsg);

    List<ToolCallAudit> all = repo.findAll();
    assertEquals(2, all.size());

    ToolCallAudit row1 = all.get(0);
    ToolCallAudit row2 = all.get(1);

    String err1 = (String)
        ReflectionTestUtils.getField(row1, "errorMessage");
    String err2 = (String)
        ReflectionTestUtils.getField(row2, "errorMessage");

    assertNull(err1, "null message should stay null");
    assertEquals(shortMsg, err2, "short message should not truncate");
  }

  @Test
  void findRecent_clampsLimitAndReturnsNewestFirstPage() {
    AuditService svc = new AuditService(repo);

    // Insert three rows
    svc.save("m1", "t", "a", true, 10L, null, null);
    svc.save("m2", "t", "a", true, 20L, null, null);
    svc.save("m3", "t", "a", false, 30L, 500, "err");

    List<ToolCallAudit> twoRows = svc.findRecent(2);
    assertEquals(2, twoRows.size());

    List<ToolCallAudit> oneRow = svc.findRecent(0);
    assertEquals(1, oneRow.size(), "limit <= 0 clamps to 1");

    List<ToolCallAudit> allRows = svc.findRecent(999);
    assertEquals(3, allRows.size(),
        "limit > row count still returns all rows");
  }

  @Test
  void summarizeByToolSince_nullSinceGroupsAndSorts() {
    AuditService svc = new AuditService(repo);

    // Row 1: tool "alpha"
    ToolCallAudit r1 = new ToolCallAudit(
        "tools/call", "alpha", "acct",
        true, 100L, null, null);
    Instant t1 = Instant.now().minusSeconds(7200L); // 2h ago
    ReflectionTestUtils.setField(r1, "createdAt", t1);
    repo.save(r1);

    // Row 2: tool "alpha" (error, later)
    ToolCallAudit r2 = new ToolCallAudit(
        "tools/call", "alpha", "acct",
        false, 300L, 500, "boom");
    Instant t2 = Instant.now().minusSeconds(3600L); // 1h ago
    ReflectionTestUtils.setField(r2, "createdAt", t2);
    repo.save(r2);

    // Row 3: tool name null -> should appear as "(none)"
    ToolCallAudit r3 = new ToolCallAudit(
        "tools/call", null, "acct",
        true, 50L, null, null);
    Instant t3 = Instant.now().minusSeconds(1800L); // 30m ago
    ReflectionTestUtils.setField(r3, "createdAt", t3);
    repo.save(r3);

    List<AuditSummary> summaries = svc.summarizeByToolSince(null);

    assertEquals(2, summaries.size(), "alpha + (none)");

    AuditSummary alpha = summaries.get(0);
    AuditSummary none = summaries.get(1);

    // alpha stats
    assertEquals("alpha", alpha.toolName());
    assertEquals(2L, alpha.totalCalls());
    assertEquals(1L, alpha.okCalls());
    assertEquals(1L, alpha.errorCalls());
    assertEquals(200L, alpha.avgDurationMs(),
        "avg of 100 and 300 should be 200");
    assertEquals(t2, alpha.lastCallAt());

    // "(none)" stats
    assertEquals("(none)", none.toolName());
    assertEquals(1L, none.totalCalls());
    assertEquals(1L, none.okCalls());
    assertEquals(0L, none.errorCalls());
    assertEquals(50L, none.avgDurationMs());
    assertEquals(t3, none.lastCallAt());
  }

  @Test
  void summarizeByToolSince_appliesCutoffInstant() {
    AuditService svc = new AuditService(repo);

    Instant now = Instant.now();

    // Old row (10h ago) -> should be filtered out for 2h window
    ToolCallAudit oldRow = new ToolCallAudit(
        "tools/call", "recent_tool", "acct",
        true, 100L, null, null);
    Instant oldTime = now.minusSeconds(36000L); // 10h
    ReflectionTestUtils.setField(oldRow, "createdAt", oldTime);
    repo.save(oldRow);

    // New row (1h ago) -> kept
    ToolCallAudit newRow = new ToolCallAudit(
        "tools/call", "recent_tool", "acct",
        true, 200L, null, null);
    Instant newTime = now.minusSeconds(3600L); // 1h
    ReflectionTestUtils.setField(newRow, "createdAt", newTime);
    repo.save(newRow);

    Instant cutoff = now.minusSeconds(7200L); // 2h
    List<AuditSummary> summaries = svc.summarizeByToolSince(cutoff);

    assertEquals(1, summaries.size(), "only one row after cutoff");
    AuditSummary summary = summaries.get(0);
    assertEquals(1L, summary.totalCalls());
    assertEquals(newTime, summary.lastCallAt());
  }
}

