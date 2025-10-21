package com.team.mcp.mcp;

import com.team.mcp.audit.AuditService;
import com.team.mcp.mcp.dto.McpRequest;
import com.team.mcp.mcp.dto.McpResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Core MCP service that handles protocol-level operations
 * and routes to tool handlers.
 */
@Service
public final class McpService {

  /** JSON-RPC: Invalid Request (-32600). */
  private static final int ERR_INVALID_REQUEST = -32600;
  /** JSON-RPC: Invalid params (-32602). */
  private static final int ERR_INVALID_PARAMS = -32602;
  /** JSON-RPC: Method not found (-32601). */
  private static final int ERR_METHOD_NOT_FOUND = -32601;

  /** Registry of available tools. */
  private final ToolRegistry tools;

  /** Optional audit service (injected if present). */
  private AuditService audit;

  /**
   * Creates a service with a tool registry.
   *
   * @param toolRegistry registry of tools
   */
  public McpService(final ToolRegistry toolRegistry) {
    this.tools = toolRegistry;
  }

  /**
   * Optional setter injection to avoid ctor changes.
   *
   * @param auditService audit service bean (may be null)
   */
  @Autowired(required = false)
  public void setAuditService(final AuditService auditService) {
    this.audit = auditService;
  }

  /**
   * Handle a JSON-RPC MCP request.
   *
   * @param req request
   * @return JSON-RPC response carrying either a result or an error
   */
  public McpResponse handle(final McpRequest req) {
    if (req == null) {
      return McpResponse.error(null, ERR_INVALID_REQUEST,
          "Invalid Request");
    }

    final String method = req.method();
    final Object id = req.id();

    if ("initialize".equals(method)) {
      return McpResponse.result(
          id,
          Map.of(
              "protocolVersion", "2024-11-05",
              "serverInfo",
                  Map.of(
                      "name", "twitter-mcp-server",
                      "version", "1.0.0"
                  ),
              "capabilities", Map.of("tools", Map.of())
          )
      );
    }

    if ("tools/list".equals(method)) {
      return McpResponse.result(
          id,
          Map.of("tools", tools.listDescriptors())
      );
    }

    if ("tools/call".equals(method)) {
      final long started = now();

      final Map<String, Object> params = req.params();
      if (params == null) {
        auditSave("tools/call", null, null, false, started,
            ERR_INVALID_PARAMS, "Missing params");
        return McpResponse.error(id, ERR_INVALID_PARAMS,
            "Missing params");
      }

      final Object nameObj = params.get("name");
      final Object argsObj = params.get("arguments");
      if (!(nameObj instanceof String)) {
        auditSave("tools/call", null, null, false, started,
            ERR_INVALID_PARAMS, "Param 'name' required");
        return McpResponse.error(
            id,
            ERR_INVALID_PARAMS,
            "Param 'name' required"
        );
      }

      @SuppressWarnings("unchecked")
      final Map<String, Object> args =
          argsObj instanceof Map ? (Map<String, Object>) argsObj
              : Map.of();

      final String toolName = (String) nameObj;
      final String accountId = extractAccountId(args);

      final Tool tool = tools.get(toolName);
      if (tool == null) {
        auditSave("tools/call", toolName, accountId, false, started,
            ERR_INVALID_PARAMS, "Unknown tool");
        return McpResponse.error(
            id,
            ERR_INVALID_PARAMS,
            "Unknown tool: " + nameObj
        );
      }

      // success path
      final var content = tool.call(args);
      auditSave("tools/call", toolName, accountId, true, started,
          null, null);
      return McpResponse.result(
          id,
          Map.of("content", content)
      );
    }

    return McpResponse.error(id, ERR_METHOD_NOT_FOUND,
        "Method not found");
  }

  /**
   * Extract the logical account id from tool arguments, if present.
   *
   * @param args tool arguments map (may be empty)
   * @return account id string when present
   */
  private static String extractAccountId(final Map<String, Object> args) {
    final Object v = args.get("accountId");
    return v instanceof String ? (String) v : null;
  }

  /**
   * Current time in milliseconds since epoch.
   *
   * @return {@code System.currentTimeMillis()}
   */
  private static long now() {
    return System.currentTimeMillis();
  }

  /**
   * Save an audit row if the audit service is available.
   *
   * @param rpcMethod JSON-RPC method name (e.g., {@code "tools/call"})
   * @param toolName  tool name when {@code rpcMethod} is {@code tools/call}
   * @param accountId logical account id extracted from arguments (nullable)
   * @param ok        success flag
   * @param startedMs start timestamp (ms since epoch) used to compute duration
   * @param errCode   optional error code (nullable)
   * @param errMsg    optional error message (nullable)
   */
  private void auditSave(final String rpcMethod, final String toolName,
      final String accountId, final boolean ok, final long startedMs,
      final Integer errCode, final String errMsg) {
    if (audit == null) {
      return;
    }
    final long dur = now() - startedMs;
    audit.save(rpcMethod, toolName, accountId, ok, dur, errCode, errMsg);
  }
}
