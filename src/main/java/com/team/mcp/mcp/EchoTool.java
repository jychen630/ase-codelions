package com.team.mcp.mcp;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Echo tool for connectivity testing.
 */
@Component
public final class EchoTool implements Tool {

  /**
   * Tool name used by the MCP protocol.
   *
   * @return stable tool identifier
   */
  @Override
  public String name() {
    return "echo_test";
  }

  /**
   * Human-readable description of the tool.
   *
   * @return short description
   */
  @Override
  public String description() {
    return "Echo back a message";
  }

  /**
   * Execute the tool: returns "Echo: &lt;message&gt;".
   *
   * @param args input arguments; may include {@code message}
   * @return MCP content array with a single text item
   */
  @Override
  public List<Map<String, Object>> call(final Map<String, Object> args) {
    final Object msg = args.getOrDefault("message", "");
    final Map<String, Object> content =
        Map.of("type", "text", "text", "Echo: " + String.valueOf(msg));
    return List.of(content);
  }
}
