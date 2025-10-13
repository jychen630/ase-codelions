package com.twitter.mcp.controller;

import com.twitter.mcp.dto.McpRequest;
import com.twitter.mcp.dto.McpResponse;
import com.twitter.mcp.service.McpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller that handles MCP JSON-RPC requests.
 * This is the main entry point for all MCP protocol communication.
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpController {
    
    private final McpService mcpService;
    
    /**
     * Handle MCP JSON-RPC requests.
     * 
     * @param request The MCP request
     * @return The MCP response
     */
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<McpResponse> handleMcpRequest(@RequestBody McpRequest request) {
        log.info("Received MCP request: method={}, id={}", request.getMethod(), request.getId());
        
        try {
            McpResponse response = mcpService.handleRequest(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error handling MCP request", e);
            return ResponseEntity.ok(
                McpResponse.error(-32603, "Internal error: " + e.getMessage(), request.getId())
            );
        }
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("MCP Server is running");
    }
}

