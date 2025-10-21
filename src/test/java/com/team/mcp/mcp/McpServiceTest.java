package com.team.mcp.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team.mcp.mcp.dto.McpRequest;
import com.team.mcp.mcp.dto.McpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for McpService with real tools and registry.
 */
class McpServiceTest {

  private McpService service;

  @BeforeEach
  void setUp() {
    // real, lightweight tool graph
    InMemoryQuotaService quota = new InMemoryQuotaService();
    CheckQuotaTool check = new CheckQuotaTool(quota);
    EchoTool echo = new EchoTool();
    ToolRegistry registry = new ToolRegistry(echo, check);
    service = new McpService(registry);
  }

  @Test
  void initialize_returnsServerInfo() {
    McpRequest req = new McpRequest(
        "2.0", "initialize", Map.of(), 1);
    McpResponse resp = service.handle(req);
    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) resp.result();
    assertEquals("2.0", resp.jsonrpc());
    assertEquals(1, resp.id());
    assertTrue(result.containsKey("protocolVersion"));
    assertTrue(result.containsKey("serverInfo"));
  }

  @Test
  void listTools_includesEchoAndQuota() {
    McpRequest req = new McpRequest(
        "2.0", "tools/list", Map.of(), 2);
    McpResponse resp = service.handle(req);
    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) resp.result();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> tools =
        (List<Map<String, Object>>) result.get("tools");
    assertTrue(tools.stream().anyMatch(
        t -> "echo_test".equals(t.get("name"))));
    assertTrue(tools.stream().anyMatch(
        t -> "check_quota_status".equals(t.get("name"))));
  }

  @Test
  void toolsCall_echo_works() {
    Map<String, Object> params = Map.of(
        "name", "echo_test",
        "arguments", Map.of("message", "Hello MCP!")
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 3);
    McpResponse resp = service.handle(req);
    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) resp.result();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) result.get("content");
    String text = String.valueOf(content.get(0).get("text"));
    assertTrue(text.contains("Echo: Hello MCP!"));
  }

  @Test
  void toolsCall_quota_works() {
    Map<String, Object> params = Map.of(
        "name", "check_quota_status",
        "arguments", Map.of()
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 4);
    McpResponse resp = service.handle(req);
    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) resp.result();
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content =
        (List<Map<String, Object>>) result.get("content");
    String text = String.valueOf(content.get(0).get("text"));
    assertTrue(text.contains("Quota Status"));
    assertTrue(text.contains("Reads"));
    assertTrue(text.contains("Writes"));
  }

  @Test
  void unknownMethod_returnsError() {
    McpRequest req = new McpRequest("2.0", "nope", Map.of(), 5);
    McpResponse resp = service.handle(req);
    assertEquals(-32601, resp.error().code());
  }

  @Test
  void toolsCall_missingParams_returnsError() {
    McpRequest req = new McpRequest("2.0", "tools/call", null, 6);
    McpResponse resp = service.handle(req);
    assertEquals(-32602, resp.error().code());
  }

  @Test
  void toolsCall_unknownTool_returnsError() {
    Map<String, Object> params = Map.of(
        "name", "not_a_tool", "arguments", Map.of()
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 7);
    McpResponse resp = service.handle(req);
    assertEquals(-32602, resp.error().code());
  }
}
