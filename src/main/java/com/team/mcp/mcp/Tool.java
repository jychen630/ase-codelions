package com.team.mcp.mcp;

import java.util.List;
import java.util.Map;

/**
 * Simple abstraction for an MCP tool.
 */
public interface Tool {

  /**
   * Tool name as exposed via tools/list.
   *
   * @return name
   */
  String name();

  /**
   * Short description for tools/list.
   *
   * @return description
   */
  String description();

  /**
   * Execute the tool.
   *
   * @param args input arguments
   * @return list of MCP "content" items
   */
  List<Map<String, Object>> call(Map<String, Object> args);
}
