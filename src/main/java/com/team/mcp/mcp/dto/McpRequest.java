package com.team.mcp.mcp.dto;

import java.util.Map;

/**
 * Minimal JSON-RPC 2.0 request DTO for MCP.
 *
 * <p>Java {@code record}. Each component is documented below.</p>
 *
 * @param jsonrpc JSON-RPC version, e.g. {@code "2.0"}.
 * @param method method name, e.g. {@code "initialize"} or {@code "tools/call"}.
 * @param params optional params map; for {@code tools/call} it holds
 *               {@code name} and {@code arguments}. May be {@code null}.
 * @param id client-supplied request id to echo back.
 */
public record McpRequest(
    String jsonrpc,
    String method,
    Map<String, Object> params,
    Object id
) { }
