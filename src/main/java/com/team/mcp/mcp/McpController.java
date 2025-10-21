package com.team.mcp.mcp;

import com.team.mcp.mcp.dto.McpRequest;
import com.team.mcp.mcp.dto.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that handles MCP JSON-RPC requests.
 * Main entry for MCP protocol communication.
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

  /** Logger for request handling. */
  private static final Logger LOG =
      LoggerFactory.getLogger(McpController.class);

  /** JSON-RPC internal error code (per spec). */
  private static final int ERR_INTERNAL = -32603;

  /** Service for routing MCP requests. */
  private final McpService mcpService;

  /**
   * Creates a controller with the given service.
   *
   * @param service service for handling MCP requests
   */
  public McpController(final McpService service) {
    this.mcpService = service;
  }

  /**
   * Handle MCP JSON-RPC requests.
   *
   * @param request request payload
   * @return response payload
   */
  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<McpResponse> handleMcpRequest(
      @RequestBody final McpRequest request) {

    LOG.info("MCP request: method={}, id={}",
        request.method(), request.id());
    try {
      final McpResponse response = mcpService.handle(request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      LOG.error("Error handling MCP request", e);
      return ResponseEntity.ok(
          McpResponse.error(request.id(), ERR_INTERNAL,
              "Internal error: " + e.getMessage()));
    }
  }

  /**
   * Simple health endpoint for MCP.
   *
   * @return human-friendly status string
   */
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("MCP Server is running");
  }
}
