package com.team.mcp.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Registry that discovers all {@link Tool} beans and exposes them by name for
 * the MCP service. Insertion order is preserved for stable listing.
 */
@Component
public final class ToolRegistry {

  /** Map of tool name to tool instance. */
  private final Map<String, Tool> byName = new LinkedHashMap<>();

  /**
   * Primary Spring constructor: Boot injects all Tool beans found via component
   * scan.
   *
   * @param toolBeans discovered tools (never null)
   */
  @Autowired
  public ToolRegistry(final List<Tool> toolBeans) {
    for (Tool t : toolBeans) {
      byName.put(t.name(), t);
    }
  }

  /**
   * Convenience constructor for tests (same package). Not for Spring
   * autowiring.
   *
   * @param toolBeans tools provided directly (e.g., new EchoTool(),
   *     new CheckQuotaTool())
   */
  ToolRegistry(final Tool... toolBeans) {
    this(List.of(toolBeans));
  }

  /**
   * Resolve a tool by name.
   *
   * @param name tool name
   * @return the tool or {@code null} if not found
   */
  public Tool get(final String name) {
    return byName.get(name);
  }

  /**
   * Return MCP tool descriptors for tools/list.
   *
   * @return list of maps with name/description
   */
  public List<Map<String, Object>> listDescriptors() {
    final List<Map<String, Object>> out = new ArrayList<>();
    for (Map.Entry<String, Tool> e : byName.entrySet()) {
      out.add(
          Map.of(
              "name", e.getKey(),
              "description", e.getValue().description()
          )
      );
    }
    return out;
  }
}
