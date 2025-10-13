package com.twitter.mcp.service;

import com.twitter.mcp.dto.McpRequest;
import com.twitter.mcp.dto.McpResponse;
import com.twitter.mcp.dto.McpTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Core MCP service that handles protocol-level operations.
 * Routes requests to appropriate tool handlers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpService {
    
    private final Map<String, ToolHandler> toolHandlers = new HashMap<>();
    private final QuotaManagementService quotaService;
    
    /**
     * Register a tool handler.
     */
    public void registerTool(String toolName, ToolHandler handler) {
        toolHandlers.put(toolName, handler);
        log.info("Registered MCP tool: {}", toolName);
    }
    
    /**
     * Handle an MCP request.
     */
    public McpResponse handleRequest(McpRequest request) {
        String method = request.getMethod();
        Object id = request.getId();
        
        log.debug("Handling MCP method: {}", method);
        
        switch (method) {
            case "initialize":
                return handleInitialize(id);
                
            case "tools/list":
                return handleListTools(id);
                
            case "tools/call":
                return handleToolCall(request);
                
            default:
                return McpResponse.error(-32601, "Method not found: " + method, id);
        }
    }
    
    /**
     * Handle the 'initialize' method.
     */
    private McpResponse handleInitialize(Object id) {
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("serverInfo", Map.of(
            "name", "twitter-mcp-server",
            "version", "1.0.0"
        ));
        result.put("capabilities", Map.of(
            "tools", Map.of()
        ));
        
        return McpResponse.success(result, id);
    }
    
    /**
     * Handle the 'tools/list' method.
     */
    private McpResponse handleListTools(Object id) {
        List<McpTool> tools = new ArrayList<>();
        
        // Add check_quota_status tool
        tools.add(McpTool.builder()
            .name("check_quota_status")
            .description("Check the current API quota status (reads and writes remaining)")
            .inputSchema(McpTool.InputSchema.builder()
                .type("object")
                .properties(new HashMap<>())
                .required(new ArrayList<>())
                .build())
            .build());
        
        // Add echo_test tool
        tools.add(McpTool.builder()
            .name("echo_test")
            .description("Echo back the input message (for testing)")
            .inputSchema(McpTool.InputSchema.builder()
                .type("object")
                .properties(Map.of(
                    "message", McpTool.PropertySchema.builder()
                        .type("string")
                        .description("The message to echo back")
                        .build()
                ))
                .required(List.of("message"))
                .build())
            .build());
        
        Map<String, Object> result = new HashMap<>();
        result.put("tools", tools);
        
        return McpResponse.success(result, id);
    }
    
    /**
     * Handle the 'tools/call' method.
     */
    private McpResponse handleToolCall(McpRequest request) {
        Map<String, Object> params = request.getParams();
        String toolName = (String) params.get("name");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        if (arguments == null) {
            arguments = new HashMap<>();
        }
        
        log.info("Calling tool: {} with arguments: {}", toolName, arguments);
        
        // Handle built-in tools
        switch (toolName) {
            case "check_quota_status":
                return handleCheckQuotaStatus(request.getId());
                
            case "echo_test":
                return handleEchoTest(arguments, request.getId());
                
            default:
                // Check if tool is registered
                ToolHandler handler = toolHandlers.get(toolName);
                if (handler != null) {
                    try {
                        Object result = handler.handle(arguments);
                        return McpResponse.success(Map.of("content", List.of(
                            Map.of("type", "text", "text", result.toString())
                        )), request.getId());
                    } catch (Exception e) {
                        log.error("Error executing tool: " + toolName, e);
                        return McpResponse.error(-32000, "Tool execution error: " + e.getMessage(), request.getId());
                    }
                } else {
                    return McpResponse.error(-32602, "Unknown tool: " + toolName, request.getId());
                }
        }
    }
    
    /**
     * Handle check_quota_status tool.
     */
    private McpResponse handleCheckQuotaStatus(Object id) {
        Map<String, Object> quotaStatus = quotaService.getQuotaStatus();
        
        String statusText = String.format(
            "Quota Status:\n" +
            "- Reads: %d/%d remaining\n" +
            "- Writes: %d/%d remaining\n" +
            "- Reset period: %s",
            quotaStatus.get("readsRemaining"),
            quotaStatus.get("readsMax"),
            quotaStatus.get("writesRemaining"),
            quotaStatus.get("writesMax"),
            quotaStatus.get("resetPeriod")
        );
        
        return McpResponse.success(Map.of("content", List.of(
            Map.of("type", "text", "text", statusText)
        )), id);
    }
    
    /**
     * Handle echo_test tool.
     */
    private McpResponse handleEchoTest(Map<String, Object> arguments, Object id) {
        String message = (String) arguments.get("message");
        
        if (message == null || message.isEmpty()) {
            return McpResponse.error(-32602, "Missing required parameter: message", id);
        }
        
        String echoText = "Echo: " + message;
        
        return McpResponse.success(Map.of("content", List.of(
            Map.of("type", "text", "text", echoText)
        )), id);
    }
    
    /**
     * Functional interface for tool handlers.
     */
    @FunctionalInterface
    public interface ToolHandler {
        Object handle(Map<String, Object> arguments) throws Exception;
    }
}

