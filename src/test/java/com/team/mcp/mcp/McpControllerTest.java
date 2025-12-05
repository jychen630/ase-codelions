package com.team.mcp.mcp;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.mcp.mcp.dto.McpRequest;
import com.team.mcp.mcp.dto.McpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice test for McpController.
 */
@ActiveProfiles("test") 
@WebMvcTest(controllers = McpController.class)
class McpControllerTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private McpService mcpService;

  @Test
  void handle_ok_delegates_to_service() throws Exception {
    McpResponse ok = new McpResponse(
        "2.0", java.util.Map.of("ok", true), null, 1);
    when(mcpService.handle(any(McpRequest.class))).thenReturn(ok);

    String body = """
        {"jsonrpc":"2.0","method":"initialize","params":{},"id":1}
        """;

    mvc.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
          .post("/mcp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(body))
      .andExpect(org.springframework.test.web.servlet.result
          .MockMvcResultMatchers.status().isOk())
      .andExpect(org.springframework.test.web.servlet.result
          .MockMvcResultMatchers.jsonPath("$.result.ok",
              equalTo(true)))
      .andExpect(org.springframework.test.web.servlet.result
          .MockMvcResultMatchers.jsonPath("$.id", equalTo(1)));
  }

  @Test
  void handle_serviceThrows_returnsJsonRpcError() throws Exception {
    when(mcpService.handle(any(McpRequest.class)))
        .thenThrow(new RuntimeException("boom"));

    String body = """
        {"jsonrpc":"2.0","method":"initialize","params":{},"id":2}
        """;

    mvc.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
          .post("/mcp")
          .contentType(MediaType.APPLICATION_JSON)
          .content(body))
      .andExpect(org.springframework.test.web.servlet.result
          .MockMvcResultMatchers.status().isOk())
      .andExpect(org.springframework.test.web.servlet.result
          .MockMvcResultMatchers.jsonPath("$.error.code",
              equalTo(-32603)))
      .andExpect(org.springframework.test.web.servlet.result
          .MockMvcResultMatchers.jsonPath("$.id", equalTo(2)));
  }

  @Test
  void health_returnsRunningMessage() throws Exception {
    mvc.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .get("/mcp/health"))
        .andExpect(org.springframework.test.web.servlet.result
            .MockMvcResultMatchers.status().isOk())
        .andExpect(org.springframework.test.web.servlet.result
            .MockMvcResultMatchers.content()
            .string("MCP Server is running"));
  }

  @Test
  void health_returnsOkText() throws Exception {
    mvc.perform(get("/mcp/health"))
        .andExpect(status().isOk())
        .andExpect(content().string("MCP Server is running"));
  }


}
