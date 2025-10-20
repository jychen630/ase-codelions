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
    private final OAuthTokenService oauthTokenService;
    
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
        
        // Add OAuth token management tools
        tools.add(McpTool.builder()
            .name("store_oauth_token")
            .description("Store OAuth token for a client and user")
            .inputSchema(McpTool.InputSchema.builder()
                .type("object")
                .properties(Map.of(
                    "client_id", McpTool.PropertySchema.builder()
                        .type("string")
                        .description("Client identifier")
                        .build(),
                    "user_id", McpTool.PropertySchema.builder()
                        .type("string")
                        .description("User identifier")
                        .build(),
                    "access_token", McpTool.PropertySchema.builder()
                        .type("string")
                        .description("OAuth access token")
                        .build(),
                    "refresh_token", McpTool.PropertySchema.builder()
                        .type("string")
                        .description("OAuth refresh token")
                        .build(),
                    "expires_in", McpTool.PropertySchema.builder()
                        .type("integer")
                        .description("Token expiration time in seconds")
                        .build()
                ))
                .required(List.of("client_id", "user_id", "access_token"))
                .build())
            .build());
        
        tools.add(McpTool.builder()
            .name("get_oauth_token")
            .description("Retrieve OAuth token for a client and user")
            .inputSchema(McpTool.InputSchema.builder()
                .type("object")
                .properties(Map.of(
                    "client_id", McpTool.PropertySchema.builder()
                        .type("string")
                        .description("Client identifier")
                        .build(),
                    "user_id", McpTool.PropertySchema.builder()
                        .type("string")
                        .description("User identifier")
                        .build()
                ))
                .required(List.of("client_id", "user_id"))
                .build())
            .build());
        
        tools.add(McpTool.builder()
            .name("delete_oauth_token")
            .description("Delete OAuth token for a client and user")
            .inputSchema(McpTool.InputSchema.builder()
                .type("object")
                .properties(Map.of(
                    "client_id", McpTool.PropertySchema.builder()
                        .type("string")
                        .description("Client identifier")
                        .build(),
                    "user_id", McpTool.PropertySchema.builder()
                        .type("string")
                        .description("User identifier")
                        .build()
                ))
                .required(List.of("client_id", "user_id"))
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
                
            case "store_oauth_token":
                return handleStoreOAuthToken(arguments, request.getId());
                
            case "get_oauth_token":
                return handleGetOAuthToken(arguments, request.getId());
                
            case "delete_oauth_token":
                return handleDeleteOAuthToken(arguments, request.getId());
                
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
     * Handle store_oauth_token tool.
     */
    private McpResponse handleStoreOAuthToken(Map<String, Object> arguments, Object id) {
        String clientId = (String) arguments.get("client_id");
        String userId = (String) arguments.get("user_id");
        String accessToken = (String) arguments.get("access_token");
        String refreshToken = (String) arguments.get("refresh_token");
        Integer expiresIn = (Integer) arguments.get("expires_in");
        
        if (clientId == null || userId == null || accessToken == null) {
            return McpResponse.error(-32602, "Missing required parameters: client_id, user_id, access_token", id);
        }
        
        try {
            // Calculate expiration time
            java.time.LocalDateTime expiresAt = null;
            if (expiresIn != null) {
                expiresAt = java.time.LocalDateTime.now().plusSeconds(expiresIn);
            }
            
            // Create token object
            com.twitter.mcp.model.OAuthToken token = com.twitter.mcp.model.OAuthToken.builder()
                .clientId(clientId)
                .userId(userId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .build();
            
            // Save token
            oauthTokenService.saveToken(token);
            
            String resultText = String.format("OAuth token stored successfully for client: %s, user: %s", clientId, userId);
            
            return McpResponse.success(Map.of("content", List.of(
                Map.of("type", "text", "text", resultText)
            )), id);
            
        } catch (Exception e) {
            log.error("Error storing OAuth token", e);
            return McpResponse.error(-32000, "Failed to store OAuth token: " + e.getMessage(), id);
        }
    }
    
    /**
     * Handle get_oauth_token tool.
     */
    private McpResponse handleGetOAuthToken(Map<String, Object> arguments, Object id) {
        String clientId = (String) arguments.get("client_id");
        String userId = (String) arguments.get("user_id");
        
        if (clientId == null || userId == null) {
            return McpResponse.error(-32602, "Missing required parameters: client_id, user_id", id);
        }
        
        try {
            java.util.Optional<com.twitter.mcp.model.OAuthToken> tokenOpt = oauthTokenService.getToken(clientId, userId);
            
            if (tokenOpt.isEmpty()) {
                return McpResponse.success(Map.of("content", List.of(
                    Map.of("type", "text", "text", "No OAuth token found for client: " + clientId + ", user: " + userId)
                )), id);
            }
            
            com.twitter.mcp.model.OAuthToken token = tokenOpt.get();
            String resultText = String.format(
                "OAuth Token Found:\n- Client ID: %s\n- User ID: %s\n- Token Type: %s\n- Expires At: %s\n- Is Expired: %s",
                token.getClientId(),
                token.getUserId(),
                token.getTokenType(),
                token.getExpiresAt(),
                token.isExpired()
            );
            
            return McpResponse.success(Map.of("content", List.of(
                Map.of("type", "text", "text", resultText)
            )), id);
            
        } catch (Exception e) {
            log.error("Error retrieving OAuth token", e);
            return McpResponse.error(-32000, "Failed to retrieve OAuth token: " + e.getMessage(), id);
        }
    }
    
    /**
     * Handle delete_oauth_token tool.
     */
    private McpResponse handleDeleteOAuthToken(Map<String, Object> arguments, Object id) {
        String clientId = (String) arguments.get("client_id");
        String userId = (String) arguments.get("user_id");
        
        if (clientId == null || userId == null) {
            return McpResponse.error(-32602, "Missing required parameters: client_id, user_id", id);
        }
        
        try {
            oauthTokenService.deleteToken(clientId, userId);
            
            String resultText = String.format("OAuth token deleted successfully for client: %s, user: %s", clientId, userId);
            
            return McpResponse.success(Map.of("content", List.of(
                Map.of("type", "text", "text", resultText)
            )), id);
            
        } catch (Exception e) {
            log.error("Error deleting OAuth token", e);
            return McpResponse.error(-32000, "Failed to delete OAuth token: " + e.getMessage(), id);
        }
    }
    
    /**
     * Functional interface for tool handlers.
     */
    @FunctionalInterface
    public interface ToolHandler {
        Object handle(Map<String, Object> arguments) throws Exception;
    }
}

