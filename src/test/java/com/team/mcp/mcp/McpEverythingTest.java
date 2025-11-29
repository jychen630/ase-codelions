package com.team.mcp.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.team.mcp.audit.AuditService;
import com.team.mcp.mcp.dto.McpRequest;
import com.team.mcp.mcp.dto.McpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * “Full graph” test that wires controller + service + tools together,
 * to make sure every public method and most branches in the mcp package
 * are actually executed.
 */
class McpEverythingTest {

  @Test
  void endToEnd_calls_all_tools_and_controller_paths() {
    // ---- Real quota + tools ----
    InMemoryQuotaService quota = new InMemoryQuotaService();
    CheckQuotaTool check = new CheckQuotaTool(quota);
    EchoTool echo = new EchoTool();

    // Minimal in-memory auth.TokenStore for token tools
    com.team.mcp.auth.TokenStore memStore =
        new com.team.mcp.auth.TokenStore() {
          private final java.util.Map<String, String> m =
              new java.util.HashMap<>();

          @Override
          public java.util.Optional<String> get(String accountId) {
            return java.util.Optional.ofNullable(m.get(accountId));
          }

          @Override
          public void put(String accountId, String token) {
            m.put(accountId, token);
          }

          @Override
          public java.util.List<String> listAccounts() {
            return new java.util.ArrayList<>(m.keySet());
          }
        };

    SetTokenTool setTokenTool = new SetTokenTool(memStore);
    GetTokenTool getTokenTool = new GetTokenTool(memStore);
    ListTokensTool listTokensTool = new ListTokensTool(memStore);

    // ---- Registry with all tools ----
    ToolRegistry registry = new ToolRegistry(
        List.of(echo, check, setTokenTool, getTokenTool, listTokensTool)
    );

    // ---- Service + audit + controller ----
    McpService service = new McpService(registry);
    AuditService audit = mock(AuditService.class);
    service.setAuditService(audit);

    McpController controller = new McpController(service);

    // 1) initialize
    McpRequest initReq =
        new McpRequest("2.0", "initialize", Map.of(), 1);
    McpResponse initResp =
        controller.handleMcpRequest(initReq).getBody();
    assertNotNull(initResp);
    assertNull(initResp.error());

    // 2) tools/list
    McpRequest listReq =
        new McpRequest("2.0", "tools/list", Map.of(), 2);
    McpResponse listResp =
        controller.handleMcpRequest(listReq).getBody();
    assertNotNull(listResp);
    assertNull(listResp.error());

    // 3) set_token via tools/call
    Map<String, Object> setArgs = Map.of(
        "name", "set_token",
        "arguments", Map.of("accountId", "acctX", "token", "tok-123")
    );
    McpRequest setReq =
        new McpRequest("2.0", "tools/call", setArgs, 3);
    McpResponse setResp =
        controller.handleMcpRequest(setReq).getBody();
    assertNotNull(setResp);
    assertNull(setResp.error());

    // 4) get_token (success, present == true)
    Map<String, Object> getArgs = Map.of(
        "name", "get_token",
        "arguments", Map.of("accountId", "acctX")
    );
    McpRequest getReq =
        new McpRequest("2.0", "tools/call", getArgs, 4);
    McpResponse getResp =
        controller.handleMcpRequest(getReq).getBody();
    assertNotNull(getResp);
    assertNull(getResp.error());

    // 5) list_tokens (non-empty)
    Map<String, Object> listTokArgs = Map.of(
        "name", "list_tokens",
        "arguments", Map.of()
    );
    McpRequest listTokReq =
        new McpRequest("2.0", "tools/call", listTokArgs, 5);
    McpResponse listTokResp =
        controller.handleMcpRequest(listTokReq).getBody();
    assertNotNull(listTokResp);
    assertNull(listTokResp.error());

    // 6) echo_test via tools/call
    Map<String, Object> echoArgs = Map.of(
        "name", "echo_test",
        "arguments", Map.of("message", "hi there")
    );
    McpRequest echoReq =
        new McpRequest("2.0", "tools/call", echoArgs, 6);
    McpResponse echoResp =
        controller.handleMcpRequest(echoReq).getBody();
    assertNotNull(echoResp);
    assertNull(echoResp.error());

    // 7) Unknown method through controller -> ERR_METHOD_NOT_FOUND
    McpRequest badReq =
        new McpRequest("2.0", "no_such_method", Map.of(), 7);
    McpResponse badResp =
        controller.handleMcpRequest(badReq).getBody();
    assertNotNull(badResp);
    assertNotNull(badResp.error());
    assertEquals(-32601, badResp.error().code());

    // 8) Health endpoint directly
    assertEquals("MCP Server is running",
        controller.health().getBody());

    // Make sure audit was actually used on at least one call
    verify(audit, atLeastOnce()).save(
        any(), any(), any(), anyBoolean(), anyLong(), any(), any()
    );
  }
}

