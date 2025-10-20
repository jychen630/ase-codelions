package com.twitter.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents an MCP tool definition.
 * Tools are the primary way clients interact with the MCP server.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpTool {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("inputSchema")
    private InputSchema inputSchema;
    
    /**
     * Represents the JSON Schema for tool input parameters.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputSchema {
        @JsonProperty("type")
        @Builder.Default
        private String type = "object";
        
        @JsonProperty("properties")
        private Map<String, PropertySchema> properties;
        
        @JsonProperty("required")
        private List<String> required;
    }
    
    /**
     * Represents a single property in the input schema.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertySchema {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("description")
        private String description;
    }
}

