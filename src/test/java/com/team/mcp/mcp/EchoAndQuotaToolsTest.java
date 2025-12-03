package com.team.mcp.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for small MCP tools (echo and quota).
 */
class EchoAndQuotaToolsTest {

  private static final class FakeQuota implements QuotaService {
    @Override public int readsUsed() { return 1; }
    @Override public int readsMax() { return 2; }
    @Override public int writesUsed() { return 3; }
    @Override public int writesMax() { return 4; }
    @Override public String resetPeriodIso() { return "monthly"; }
  }

  @Test
  void echo_returnsEchoedMessage() {
    var tool = new EchoTool();
    var out = tool.call(Map.of("message", "MCP!"));
    String text = (String) out.get(0).get("text");
    assertTrue(text.contains("Echo: MCP!"));
  }

  @Test
  void checkQuota_includesNumbersAndPeriod() {
    var tool = new CheckQuotaTool(new FakeQuota());
    List<Map<String, Object>> out = tool.call(Map.of());
    String text = (String) out.get(0).get("text");
    assertTrue(text.contains("Reads"));
    assertTrue(text.contains("Writes"));
    assertTrue(text.contains("monthly"));
  }

  @Test
  void echo_hasExpectedMetadata() {
    var tool = new EchoTool();
    assertEquals("echo_test", tool.name());
    assertTrue(tool.description().toLowerCase().contains("echo"));
  }

  @Test
  void checkQuota_hasExpectedMetadata() {
    var tool = new CheckQuotaTool(new FakeQuota());
    assertEquals("check_quota_status", tool.name());
    assertTrue(tool.description().toLowerCase().contains("quota"));
  }

}
