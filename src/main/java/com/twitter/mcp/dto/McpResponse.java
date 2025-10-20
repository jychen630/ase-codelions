package com.twitter.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an MCP JSON-RPC 2.0 response.
 * 
 * Success example:
 * {
 *   "jsonrpc": "2.0",
 *   "result": {"status": "success", "data": {...}},
 *   "id": 1
 * }
 * 
 * Error example:
 * {
 *   "jsonrpc": "2.0",
 *   "error": {"code": -32601, "message": "Method not found"},
 *   "id": 1
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResponse {
    
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    
    @JsonProperty("result")
    private Object result;
    
    @JsonProperty("error")
    private McpError error;
    
    @JsonProperty("id")
    private Object id;
    
    /**
     * Create a success response.
     */
    public static McpResponse success(Object result, Object id) {
        McpResponse response = new McpResponse();
        response.jsonrpc = "2.0";
        response.result = result;
        response.id = id;
        return response;
    }
    
    /**
     * Create an error response.
     */
    public static McpResponse error(int code, String message, Object id) {
        McpResponse response = new McpResponse();
        response.jsonrpc = "2.0";
        response.error = new McpError(code, message);
        response.id = id;
        return response;
    }
    
    /**
     * Represents a JSON-RPC error object.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpError {
        @JsonProperty("code")
        private int code;
        
        @JsonProperty("message")
        private String message;
    }
}

