package com.team.mcp.audit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
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
    svc.save("tools/call", "search_tweets", "acctA", false, 123L, 400, longMsg);

    List<ToolCallAudit> all = repo.findAll();
    assertEquals(1, all.size());
    ToolCallAudit row = all.get(0);

    assertNotNull(row.getId());

    // Access private fields (no public getters)
    String rpcMethod = (String) ReflectionTestUtils.getField(row, "rpcMethod");
    String toolName  = (String) ReflectionTestUtils.getField(row, "toolName");
    Integer code     = (Integer)ReflectionTestUtils.getField(row, "errorCode");
    String errMsg    = (String) ReflectionTestUtils.getField(row, "errorMessage");
    Object createdAt = ReflectionTestUtils.getField(row, "createdAt");

    assertEquals("tools/call", rpcMethod);
    assertEquals("search_tweets", toolName);
    assertEquals(400, code);
    assertNotNull(createdAt);
    assertNotNull(errMsg);
    assertEquals(ToolCallAudit.LEN_ERROR, errMsg.length(), "should be truncated");
  }

  @Test
  void save_nullErrorMessage_handlesGracefully() {
    AuditService svc = new AuditService(repo);
    svc.save("tools/call", "echo", "acct", true, 50L, null, null);

    List<ToolCallAudit> all = repo.findAll();
    assertEquals(1, all.size());
    ToolCallAudit row = all.get(0);
    String errMsg = (String) ReflectionTestUtils.getField(row, "errorMessage");
    assertNull(errMsg, "null error message should remain null");
  }

  @Test
  void save_shortErrorMessage_notTruncated() {
    AuditService svc = new AuditService(repo);
    String shortMsg = "short";
    svc.save("tools/call", "echo", "acct", false, 50L, 400, shortMsg);

    List<ToolCallAudit> all = repo.findAll();
    assertEquals(1, all.size());
    ToolCallAudit row = all.get(0);
    String errMsg = (String) ReflectionTestUtils.getField(row, "errorMessage");
    assertEquals(shortMsg, errMsg, "short message should not be truncated");
  }
}
