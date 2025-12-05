package com.team.mcp.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team.mcp.mcp.dto.McpRequest;
import com.team.mcp.mcp.dto.McpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

  @Test
  void handle_nullRequest_returnsError() {
    McpResponse resp = service.handle(null);
    assertEquals(-32600, resp.error().code());
    assertEquals("Invalid Request", resp.error().message());
  }

  @Test
  void toolsCall_nonStringName_returnsError() {
    Map<String, Object> params = Map.of(
        "name", 123,  // not a String
        "arguments", Map.of()
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 8);
    McpResponse resp = service.handle(req);
    assertEquals(-32602, resp.error().code());
    assertTrue(resp.error().message().contains("name"));
  }

  @Test
  void toolsCall_nonMapArguments_usesEmptyMap() {
    Map<String, Object> params = Map.of(
        "name", "echo_test",
        "arguments", "not-a-map"  // not a Map
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 9);
    McpResponse resp = service.handle(req);
    // Should still work, using empty map for arguments
    assertTrue(resp.result() != null);
  }

  @Test
  void toolsCall_withAccountId_extractsCorrectly() {
    Map<String, Object> params = Map.of(
        "name", "echo_test",
        "arguments", Map.of(
            "message", "test",
            "accountId", "test-account"
        )
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 10);
    McpResponse resp = service.handle(req);
    // Should work fine with accountId in arguments
    assertTrue(resp.result() != null);
  }

  @Test
  void toolsCall_nonStringAccountId_handlesGracefully() {
    Map<String, Object> params = Map.of(
        "name", "echo_test",
        "arguments", Map.of(
            "message", "test",
            "accountId", 123  // not a String
        )
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 11);
    McpResponse resp = service.handle(req);
    // Should still work, accountId extraction returns null for non-String
    assertTrue(resp.result() != null);
  }

  @Test
  void handle_withAuditService_recordsCalls() {
    com.team.mcp.audit.AuditService audit = mock(com.team.mcp.audit.AuditService.class);
    InMemoryQuotaService quota = new InMemoryQuotaService();
    CheckQuotaTool check = new CheckQuotaTool(quota);
    EchoTool echo = new EchoTool();
    ToolRegistry registry = new ToolRegistry(echo, check);
    McpService svcWithAudit = new McpService(registry);
    svcWithAudit.setAuditService(audit);

    Map<String, Object> params = Map.of(
        "name", "echo_test",
        "arguments", Map.of("message", "test")
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 12);
    McpResponse resp = svcWithAudit.handle(req);

    assertTrue(resp.result() != null);
    verify(audit).save(eq("tools/call"), eq("echo_test"), any(), eq(true), anyLong(), isNull(), isNull());
  }

  @Test
  void handle_withoutAuditService_worksFine() {
    // Service without audit should work normally
    Map<String, Object> params = Map.of(
        "name", "echo_test",
        "arguments", Map.of("message", "test")
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 13);
    McpResponse resp = service.handle(req);
    assertTrue(resp.result() != null);
  }

  @Test
  void toolsCall_emptyArguments_usesEmptyMap() {
    Map<String, Object> params = Map.of(
        "name", "echo_test"
        // no "arguments" key
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 14);
    McpResponse resp = service.handle(req);
    assertTrue(resp.result() != null);
  }

  @Test
  void handle_withAuditService_recordsErrorCases() {
    com.team.mcp.audit.AuditService audit = mock(com.team.mcp.audit.AuditService.class);
    InMemoryQuotaService quota = new InMemoryQuotaService();
    CheckQuotaTool check = new CheckQuotaTool(quota);
    EchoTool echo = new EchoTool();
    ToolRegistry registry = new ToolRegistry(echo, check);
    McpService svcWithAudit = new McpService(registry);
    svcWithAudit.setAuditService(audit);

    // Test error case with audit
    McpRequest req = new McpRequest("2.0", "tools/call", null, 15);
    McpResponse resp = svcWithAudit.handle(req);

    assertEquals(-32602, resp.error().code());
    verify(audit).save(eq("tools/call"), isNull(), isNull(), eq(false), anyLong(), 
        eq(-32602), eq("Missing params"));
  }

  @Test
  void handle_withAuditService_recordsUnknownTool() {
    com.team.mcp.audit.AuditService audit = mock(com.team.mcp.audit.AuditService.class);
    InMemoryQuotaService quota = new InMemoryQuotaService();
    CheckQuotaTool check = new CheckQuotaTool(quota);
    EchoTool echo = new EchoTool();
    ToolRegistry registry = new ToolRegistry(echo, check);
    McpService svcWithAudit = new McpService(registry);
    svcWithAudit.setAuditService(audit);

    Map<String, Object> params = Map.of("name", "unknown_tool", "arguments", Map.of());
    McpRequest req = new McpRequest("2.0", "tools/call", params, 16);
    McpResponse resp = svcWithAudit.handle(req);

    assertEquals(-32602, resp.error().code());
    verify(audit).save(eq("tools/call"), eq("unknown_tool"), isNull(), eq(false), 
        anyLong(), eq(-32602), contains("Unknown tool"));
  }

  @Test
  void handle_withAuditService_recordsInvalidName() {
    com.team.mcp.audit.AuditService audit = mock(com.team.mcp.audit.AuditService.class);
    InMemoryQuotaService quota = new InMemoryQuotaService();
    CheckQuotaTool check = new CheckQuotaTool(quota);
    EchoTool echo = new EchoTool();
    ToolRegistry registry = new ToolRegistry(echo, check);
    McpService svcWithAudit = new McpService(registry);
    svcWithAudit.setAuditService(audit);

    Map<String, Object> params = Map.of("name", 123, "arguments", Map.of());
    McpRequest req = new McpRequest("2.0", "tools/call", params, 17);
    McpResponse resp = svcWithAudit.handle(req);

    assertEquals(-32602, resp.error().code());
    verify(audit).save(eq("tools/call"), isNull(), isNull(), eq(false), 
        anyLong(), eq(-32602), contains("name"));
  }
}
