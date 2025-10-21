# Module: `mcp/mcp/` — MCP server, JSON-RPC, and built-in tools

This module is the **entry point for AI/agent integrations**. It exposes a single HTTP endpoint that speaks **JSON-RPC 2.0** and a registry of **tools** the agent can call. What sits here is completely framework-agnostic from the AI point of view: if a client can POST JSON to `/mcp`, it can “use” our system.

---

## What’s in this directory

`src/main/java/com/team/mcp/mcp/`

* **`McpController.java`**
  Spring MVC controller that maps `POST /mcp` and delegates to `McpService`.

* **`McpService.java`**
  The protocol engine. Parses JSON-RPC requests and returns JSON-RPC responses.
  Implements:

  * `initialize` → returns protocolVersion, serverInfo, and capability flags.
  * `tools/list` → returns the catalog of tools (name, description, input schema).
  * `tools/call` → validates params, dispatches to the named tool, returns the tool result.
    Error codes used (JSON-RPC):
  * `-32600` Invalid Request
  * `-32601` Method not found
  * `-32602` Invalid params
  * `-32603` Internal error
    Notes: if the optional `AuditService` bean is present (from `audit/`), `McpService` records a row per call.

* **`Tool.java`**
  Small SPI: `name()`, `description()`, `schema()` (optional), and `call(Map<String,Object> args)`.

* **`ToolRegistry.java`**
  Holds all active tools and exposes them to `McpService` for `tools/list` and `tools/call`.

* **Tool implementations (shipped in this module)**

  * `EchoTool.java` – echos back provided arguments (great for smoke tests).
  * `CheckQuotaTool.java` – returns current “quota” snapshot from `QuotaService`.
  * `GetTokenTool.java` – reads a stored token for an account from a `TokenStore`.
  * `SetTokenTool.java` – writes/updates a token in the `TokenStore`.
  * `ListTokensTool.java` – lists known accounts present in the `TokenStore`.

* **Support services (local, in-memory defaults)**

  * `InMemoryQuotaService.java` and `QuotaService.java` – simple metrics stub.
  * `InMemoryTokenStore.java` and `TokenStore.java` – simple token persistence stub used by the tools above.
    In production we swap this for the DB-backed store from `auth/`.

* **`dto/`**

  * `McpRequest.java`, `McpResponse.java` – minimal JSON-RPC POJOs used by the controller/service.

---

## How to run this module

Start the whole app as usual (this module is part of the Spring Boot application):

```bash
mvn -q spring-boot:run \
  -Dspring-boot.run.profiles=devdb \
  -Dspring-boot.run.jvmArguments="-Dapp.search.source=db"
```

Endpoint: `POST http://localhost:8080/mcp`
Content-Type: `application/json`

If you enabled request auth in `security/` (e.g., `TokenProvider`), include the expected header (for local dev we typically allow it to be absent).

---

## JSON-RPC contract (quick refresher)

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": { "name": "echo", "arguments": { "text": "hi" } }
}
```

Response:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": { "content": [ { "type": "text", "text": "hi" } ] }
}
```

On error:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "error": { "code": -32602, "message": "Invalid params" }
}
```

---

## End-to-end test scriptlets (curl)

All commands POST to `/mcp`. Use `jq` just to pretty-print.

### 1) initialize

```bash
curl -s localhost:8080/mcp -H 'Content-Type: application/json' -d '{
  "jsonrpc":"2.0", "id":1, "method":"initialize", "params":{}
}' | jq
```

What you should see: `protocolVersion`, `serverInfo` and empty `capabilities.tools` object.
This proves the endpoint is up and JSON-RPC parsing works.

### 2) tools/list

```bash
curl -s localhost:8080/mcp -H 'Content-Type: application/json' -d '{
  "jsonrpc":"2.0", "id":2, "method":"tools/list", "params":{}
}' | jq
```

What you should see: a `tools` array with `echo`, `check_quota_status`, `get_token`, `set_token`, `list_tokens` (and any other tools registered by `ToolRegistry`).

### 3) echo (sanity tool)

```bash
curl -s localhost:8080/mcp -H 'Content-Type: application/json' -d '{
  "jsonrpc":"2.0",
  "id":3,
  "method":"tools/call",
  "params":{ "name":"echo", "arguments":{"text":"Hello MCP"} }
}' | jq
```

Expect: the same text returned in `result.content`.

### 4) token storage tools (in-memory by default)

Set a token:

```bash
curl -s localhost:8080/mcp -H 'Content-Type: application/json' -d '{
  "jsonrpc":"2.0",
  "id":10,
  "method":"tools/call",
  "params":{
    "name":"set_token",
    "arguments":{"accountId":"acctA","token":"abc123"}
  }
}' | jq
```

Get it back:

```bash
curl -s localhost:8080/mcp -H 'Content-Type: application/json' -d '{
  "jsonrpc":"2.0",
  "id":11,
  "method":"tools/call",
  "params":{
    "name":"get_token",
    "arguments":{"accountId":"acctA"}
  }
}' | jq
```

List known accounts:

```bash
curl -s localhost:8080/mcp -H 'Content-Type: application/json' -d '{
  "jsonrpc":"2.0",
  "id":12,
  "method":"tools/call",
  "params":{ "name":"list_tokens", "arguments":{} }
}' | jq
```

Short explanation: these prove an agent can persist and retrieve credentials through a generic `TokenStore`. For Iteration-1 we use `InMemoryTokenStore`; in Iteration-2 we point these tools at the DB-backed store from `auth/` so tokens survive restarts.

### 5) quota tool

```bash
curl -s localhost:8080/mcp -H 'Content-Type: application/json' -d '{
  "jsonrpc":"2.0",
  "id":20,
  "method":"tools/call",
  "params":{ "name":"check_quota_status", "arguments":{} }
}' | jq
```

What you get: a small JSON snapshot (remaining, used, resetAt). It’s a stub, but demonstrates how an LLM can call read-only operational tools.

---

## How this module wires into the rest of the codebase

* **Search & analytics tools**
  If you register tools that wrap `SearchService` or `AnalyticsService` (e.g., a `search_tweets` tool class that calls `SearchService.search(...)`), `ToolRegistry` will list them automatically and `tools/call` will route to them. In this repo you already have a `SearchTweetsTool` under `search/` that can be added to the registry; once it’s a Spring bean, it appears in `tools/list`.

* **Scheduling**
  We currently expose scheduling via an HTTP helper under `/tools/schedule_tweet` for demos. If you later provide a `ScheduleTweetTool` and register it, agents will schedule posts directly through `/mcp` as well.

* **Auth/OAuth**
  The token tools in this module currently use `InMemoryTokenStore`. When you switch to the real DB token store from `auth/`, wire that `TokenStore` bean so `Get/Set/ListTokenTool` operate on persistent data.

* **Audit**
  If `AuditService` from `audit/` is on the classpath (it is) and available in the Spring context, `McpService` records each JSON-RPC call (method, tool name, account, ok/error). This gives you per-tool telemetry without changing tool code.

---

## Expected behavior now vs. Iteration-2

* **Now (Iteration-1)**

  * Fully functional JSON-RPC MCP endpoint.
  * Tool discovery and dispatch.
  * Echo, quota, and token tools working.
  * Optional auditing of calls if `audit/` is active.

* **Iteration-2**

  * Point `TokenStore` to the DB implementation in `auth/` so tokens persist.
  * Add production tools that call real services: `search_tweets`, `top_hashtags`, `best_hours`, `schedule_tweet`, etc.
  * Harden error surface: richer tool schemas, input validation messages, and per-tool error codes.

---

## Troubleshooting

* **“Method not found”** – you posted a JSON-RPC method other than `initialize`, `tools/list`, or `tools/call`.
* **“Unknown tool”** – `ToolRegistry` doesn’t have a bean with that name. Ensure your tool class implements `Tool`, has a unique `name()`, and is a Spring bean.
* **Null results** – your tool returned `null` or threw. Check application logs; `McpService` wraps unexpected exceptions in `-32603` Internal error.
* **Auth header rejected** – if `security/` enforces a header, include the expected token for dev or disable the filter locally.

