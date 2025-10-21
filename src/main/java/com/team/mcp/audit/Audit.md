# Audit

## What it does

The Audit feature records operational breadcrumbs for tool invocations and HTTP requests:

* **Tool call audit (persistent):** every `/mcp` **tools/call** request writes one row to the database with the method name, tool name, account id (if present), success flag, error code/message (if any), duration, and timestamp.
* **HTTP request logging (non-persistent):** every incoming HTTP request is logged with path, method, status, and duration to the application log.

This gives us two complementary views: durable DB records for MCP tool usage, and lightweight logs for all web traffic.

---

## Where the code lives

```
src/main/java/com/team/mcp/audit/
  AuditService.java            -- small facade for saving audit rows
  ToolCallAudit.java           -- @Entity for the audit row
  ToolCallAuditRepository.java -- Spring Data JPA repository
  WebRequestLogFilter.java     -- logs every HTTP request (non-persistent)
  package-info.java            -- package docs
```

**How it’s wired:**

* `McpService` (in `src/main/java/com/team/mcp/mcp/`) calls `audit.save(...)` on every **tools/call** code path, both success and error. Injection uses an optional setter (`@Autowired(required = false)`), so the service will run even if auditing were disabled.
* `WebRequestLogFilter` extends `OncePerRequestFilter` and is registered as a `@Component`, so it logs each request automatically.

**Database shape (table `tool_call_audit`):**

* `id` (PK), `rpc_method`, `tool_name`, `account_id`, `ok`, `duration_ms`, `error_code`, `error_message`, `created_at`
* Column sizes are defined as named constants in `ToolCallAudit` to avoid magic numbers.

> In Iteration 1 we created this via JPA entity auto-DDL or Flyway, depending on your local profile. In `devdb` profile you’re already running Flyway for core tables; if `tool_call_audit` is missing on a teammate’s machine, either add a migration for it or temporarily enable `spring.jpa.hibernate.ddl-auto=update` to bootstrap.

---

## How to run it

Start the app in DB mode so audits persist:

```bash
mvn -q spring-boot:run \
  -Dspring-boot.run.profiles=devdb \
  -Dspring-boot.run.jvmArguments="-Dapp.search.source=db"
```

H2 console: `http://localhost:8080/h2-console`
JDBC URL: `jdbc:h2:file:./.h2/mcp;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL`
User: `sa`  (empty password)

---

## Quick tour of behaviors

### A) HTTP log lines (non-persistent)

The filter prints a single line after each request:

```
INFO  WebRequestLogFilter : http GET /search -> 200 (12 ms)
```

You’ll see these for every endpoint, including `/mcp`, `/search`, `/auth/*`, and the H2 console.

### B) Tool audit rows (persistent)

`McpService.handle(...)` records on **every** `tools/call` path:

* **Success row**: `ok=true`, `error_code=null`, `error_message=null`
* **Error row**: `ok=false`, with the appropriate JSON-RPC code and message
* Duration is measured wall-clock in milliseconds

---

## End-to-end examples

### 1) Happy path: call a known tool

List the server’s tools, pick one, then call it.

```bash
# Discover tool names
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{ "jsonrpc":"2.0", "id": 1, "method":"tools/list" }' | jq .

# Call a tool (example: search_tweets or echo_test; adjust to your tool set)
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc":"2.0","id":2,"method":"tools/call",
    "params":{"name":"echo_test","arguments":{"text":"hello"}}
  }' | jq .
```

**What to expect**

* Console log: one HTTP line for `/mcp`.
* DB row: one `tool_call_audit` with `rpc_method='tools/call'`, `tool_name='echo_test'`, `ok=true`, `error_code=NULL`.

**Verify in H2**

```sql
SELECT id, rpc_method, tool_name, account_id, ok, duration_ms, error_code, error_message, created_at
FROM tool_call_audit
ORDER BY id DESC
LIMIT 5;
```

### 2) Error path: unknown tool

```bash
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc":"2.0","id":3,"method":"tools/call",
    "params":{"name":"no_such_tool","arguments":{}}
  }' | jq .
```

**What to expect**

* JSON-RPC error: code `-32602` (invalid params) with message “Unknown tool”.
* DB row: `ok=false`, `error_code=-32602`, `error_message` “Unknown tool”, reasonable `duration_ms`.

### 3) Error path: missing params

```bash
# No "params" at all
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{ "jsonrpc":"2.0","id":4,"method":"tools/call" }' | jq .
```

**What to expect**

* JSON-RPC error: `-32602` “Missing params”.
* DB row: `ok=false`, `error_code=-32602`, message “Missing params”.

### 4) Account-aware tool call

If the tool accepts `accountId`, we store it in the audit row:

```bash
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc":"2.0","id":5,"method":"tools/call",
    "params":{"name":"search_tweets","arguments":{
      "accountId":"acctA",
      "q":"hello",
      "limit":5
    }}
  }' | jq .
```

**Verify in H2**

```sql
SELECT tool_name, account_id, ok, created_at
FROM tool_call_audit
ORDER BY id DESC
LIMIT 5;
```

You should see `account_id='acctA'`.

---

## Operational tips

* **Noise control:** HTTP logs are INFO; reduce via `logging.level.com.team.mcp.audit=INFO` or `WARN` globally if needed.
* **Payload safety:** We intentionally do **not** store full request/response bodies to avoid leaking tokens or user text. Only metadata and errors are recorded.
* **Table growth:** For a long-running server, add retention (e.g., nightly deletion of rows older than N days). For Iteration 1, manual cleanup is fine:

  ```sql
  DELETE FROM tool_call_audit WHERE created_at < DATEADD('DAY', -7, CURRENT_TIMESTAMP());
  ```

---

## What changes in Iteration 2

* **Sensitive redaction and sampling:** keep storing metadata, but add optional payload snippets with automated redaction and sampling to control volume.
* **Correlation IDs:** put a request ID in MDC and columns to tie HTTP logs, tool audits, and downstream calls together.
* **Dashboards/metrics:** aggregate counts per tool, error rates, p95 durations; expose a lightweight `/metrics/audit` or ship to Prometheus.
* **Audit for other subsystems:** add similar DB-backed audit for scheduling runs and OAuth flows (success/error, provider codes).

---

## Quick reference

* **Persistent audit API:** `AuditService.save(...)`
  Called from `McpService.auditSave(...)` on all `/mcp` **tools/call** paths.
* **Entity/Repository:** `ToolCallAudit`, `ToolCallAuditRepository`
* **HTTP logging:** `WebRequestLogFilter` (console only)

This is the exact behavior we exercised while testing the MCP flows and search tools: every tool invocation appears once in `tool_call_audit`, while all HTTP traffic is visible in the logs with timing and status.
