package com.twitter.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Represents an MCP JSON-RPC 2.0 request.
 * 
 * Example:
 * {
 *   "jsonrpc": "2.0",
 *   "method": "tools/call",
 *   "params": {
 *     "name": "post_tweet",
 *     "arguments": {"content": "Hello World"}
 *   },
 *   "id": 1
 * }
 */
public class McpRequest {
    
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("params")
    private Map<String, Object> params;
    
    @JsonProperty("id")
    private Object id;
    
    public McpRequest() {}
    
    public McpRequest(String jsonrpc, String method, Map<String, Object> params, Object id) {
        this.jsonrpc = jsonrpc;
        this.method = method;
        this.params = params;
        this.id = id;
    }
    
    public String getJsonrpc() { return jsonrpc; }
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }
    
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
    
    public Object getId() { return id; }
    public void setId(Object id) { this.id = id; }
}

