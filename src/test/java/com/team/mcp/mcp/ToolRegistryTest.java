package com.team.mcp.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ToolRegistry.
 */
class ToolRegistryTest {

  @Test
  void registry_lists_and_resolves_tools() {
    InMemoryQuotaService quota = new InMemoryQuotaService();
    ToolRegistry reg =
        new ToolRegistry(new EchoTool(), new CheckQuotaTool(quota));

    List<Map<String, Object>> list = reg.listDescriptors();
    // two built-ins
    assertEquals(2, list.size());
    assertNotNull(reg.get("echo_test"));
    assertNotNull(reg.get("check_quota_status"));
  }

  @Test
  void springStyleConstructor_populatesRegistry() {
    InMemoryQuotaService quota = new InMemoryQuotaService();
    java.util.List<Tool> beans =
        java.util.List.of(new EchoTool(), new CheckQuotaTool(quota));

    ToolRegistry reg = new ToolRegistry(beans);

    assertNotNull(reg.get("echo_test"));
    assertNotNull(reg.get("check_quota_status"));
    assertEquals(2, reg.listDescriptors().size());
  }

}
