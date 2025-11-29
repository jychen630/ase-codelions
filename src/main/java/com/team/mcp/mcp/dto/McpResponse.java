package com.team.mcp.mcp.dto;

import java.util.Map;

/**
 * Minimal JSON-RPC 2.0 response DTO for MCP.
 *
 * <p>This is modeled as a Java {@code record} so each component is a
 * public final field with an accessor. Checkstyle requires documenting
 * each component using {@code @param} tags.</p>
 *
 * @param jsonrpc the JSON-RPC protocol version (e.g. {@code "2.0"})
 * @param result  the success payload when the call succeeds; null on error
 * @param error   the error payload when the call fails; null on success
 * @param id      the request identifier echoed back to the client
 */
public record McpResponse(
    String jsonrpc,
    Object result,
    ErrorObj error,
    Object id
) {

  /**
   * Build a success response.
   *
   * @param id request id
   * @param result result object (placed under {@code result})
   * @return response
   */
  public static McpResponse result(
      final Object id, final Map<String, Object> result) {
    return new McpResponse("2.0", result, null, id);
  }

  /**
   * Build an error response.
   *
   * @param id request id
   * @param code JSON-RPC error code
   * @param message human-readable error message
   * @return response
   */
  public static McpResponse error(
      final Object id, final int code, final String message) {
    return new McpResponse("2.0", null,
        new ErrorObj(code, message), id);
  }

  /**
   * Error object for JSON-RPC responses.
   *
   * @param code JSON-RPC error code
   * @param message human-readable error message
   */
  public record ErrorObj(int code, String message) { }
}
