package com.team.mcp.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import com.team.mcp.audit.AuditService;

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

  @Test
  void nullRequest_returnsInvalidRequestError() {
    McpResponse resp = service.handle(null);
    assertEquals(-32600, resp.error().code());
    assertNull(resp.id());
  }

  @Test
  void toolsCall_missingName_returnsError_andIsAudited() {
    ToolRegistry registry = new ToolRegistry(new EchoTool());
    McpService svc = new McpService(registry);
    AuditService audit = mock(AuditService.class);
    svc.setAuditService(audit);

    // params have arguments but no "name"
    Map<String, Object> params = Map.of(
        "arguments", Map.of("message", "hi")
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 8);

    McpResponse resp = svc.handle(req);
    assertEquals(-32602, resp.error().code());

    verify(audit).save(
        eq("tools/call"),
        isNull(),
        isNull(),
        eq(false),
        anyLong(),
        eq(-32602),
        anyString()
    );
  }

  @Test
  void toolsCall_success_isAuditedWithAccountId() {
    InMemoryQuotaService quota = new InMemoryQuotaService();
    CheckQuotaTool check = new CheckQuotaTool(quota);
    ToolRegistry registry = new ToolRegistry(check);
    McpService svc = new McpService(registry);
    AuditService audit = mock(AuditService.class);
    svc.setAuditService(audit);

    Map<String, Object> params = Map.of(
        "name", "check_quota_status",
        "arguments", Map.of("accountId", "acct-9")
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 9);

    McpResponse resp = svc.handle(req);
    assertNull(resp.error());

    verify(audit).save(
        eq("tools/call"),
        eq("check_quota_status"),
        eq("acct-9"),
        eq(true),
        anyLong(),
        isNull(),
        isNull()
    );
  }

  @Test
  void toolsCall_withNonMapArgumentsDefaultsToEmptyArgs() {
    ToolRegistry registry = new ToolRegistry(new EchoTool());
    McpService svc = new McpService(registry);

    Map<String, Object> params = Map.of(
        "name", "echo_test",
        // not a Map -> branch where argsObj is not Map
        "arguments", "ignored"
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 10);

    McpResponse resp = svc.handle(req);
    assertNull(resp.error());

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) resp.result();
    @SuppressWarnings("unchecked")
    var content = (java.util.List<java.util.Map<String, Object>>)
        result.get("content");

    String text = String.valueOf(content.get(0).get("text"));
    assertTrue(text.startsWith("Echo: "));
  }


  @Test
  void toolsCall_withNullArguments_usesEmptyArgs() {
    ToolRegistry registry = new ToolRegistry(new EchoTool());
    McpService svc = new McpService(registry);

    // name is present, "arguments" is completely absent -> argsObj == null
    Map<String, Object> params = Map.of("name", "echo_test");
    McpRequest req = new McpRequest("2.0", "tools/call", params, 11);

    McpResponse resp = svc.handle(req);
    assertNull(resp.error());

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) resp.result();
    @SuppressWarnings("unchecked")
    var content =
        (java.util.List<java.util.Map<String, Object>>) result.get("content");

    // message was missing, so should echo empty string
    String text = String.valueOf(content.get(0).get("text"));
    assertTrue(text.startsWith("Echo: "));
  }

  @Test
  void toolsCall_withAccountIdNonString_branchInExtractAccountId() {
    InMemoryQuotaService quota = new InMemoryQuotaService();
    CheckQuotaTool check = new CheckQuotaTool(quota);
    ToolRegistry registry = new ToolRegistry(check);
    McpService svc = new McpService(registry);
    AuditService audit = mock(AuditService.class);
    svc.setAuditService(audit);

    // accountId present but not String -> extractAccountId should return null
    Map<String, Object> params = Map.of(
        "name", "check_quota_status",
        "arguments", Map.of("accountId", 123)   // non-string
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 12);

    McpResponse resp = svc.handle(req);
    assertNull(resp.error());

    verify(audit).save(
        eq("tools/call"),
        eq("check_quota_status"),
        isNull(),          // accountId should be null here
        eq(true),
        anyLong(),
        isNull(),
        isNull()
    );
  }

  @Test
  void toolsCall_nameNotString_returnsError_andIsAudited() {
    ToolRegistry registry = new ToolRegistry(new EchoTool());
    McpService svc = new McpService(registry);
    AuditService audit = mock(AuditService.class);
    svc.setAuditService(audit);

    // "name" present but not a String
    Map<String, Object> params = Map.of(
        "name", 123,
        "arguments", Map.of("message", "hi")
    );
    McpRequest req = new McpRequest("2.0", "tools/call", params, 13);

    McpResponse resp = svc.handle(req);
    assertEquals(-32602, resp.error().code());

    verify(audit).save(
        eq("tools/call"),
        isNull(),          // toolName is null when name is invalid
        isNull(),
        eq(false),
        anyLong(),
        eq(-32602),
        anyString()
    );
  }

  @Test
  void toolsCall_missingParams_withAuditEnabled_isAudited() {
    ToolRegistry registry = new ToolRegistry(new EchoTool());
    McpService svc = new McpService(registry);
    AuditService audit = mock(AuditService.class);
    svc.setAuditService(audit);

    // params == null branch, but with audit present this time
    McpRequest req = new McpRequest("2.0", "tools/call", null, 14);

    McpResponse resp = svc.handle(req);
    assertEquals(-32602, resp.error().code());

    verify(audit).save(
        eq("tools/call"),
        isNull(),
        isNull(),
        eq(false),
        anyLong(),
        eq(-32602),
        anyString()
    );
  }


}
