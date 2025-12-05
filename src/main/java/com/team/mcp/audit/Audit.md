# **Audit – Iteration 2**

---

## **What it does**

In Iteration 2, the Audit subsystem becomes a more complete observability component.
It continues to record persistent MCP tool-call audit rows and non-persistent HTTP request logs, and it now adds two new query endpoints that expose real-time debugging and monitoring data.

Collectively, these capabilities offer:

* Persistent audit trails for every `/mcp` tools/call
* HTTP logs for every REST request (via `WebRequestLogFilter`)
* Recent-activity inspection (new)
* Per-tool analytics and performance metrics (new)
* Error tracking, including error codes and messages
* Account-aware auditing for tools that rely on accounts

This provides the full scope of auditing functionality planned for Iteration 2.

---

## **Where the code lives**

```
src/main/java/com/team/mcp/audit/
    AuditService.java            – persists audit rows
    ToolCallAudit.java           – JPA @Entity
    ToolCallAuditRepository.java – Spring Data repository
    WebRequestLogFilter.java     – logs HTTP traffic
    AuditController.java         – new in Iteration 2 (endpoints)
```

---

## **Iteration 2 Additions**

---

### **1. `/audit/recent` — Recent audit rows (NEW)**

**Endpoint:**

```
GET /audit/recent?limit=N
```

Returns the N most recent MCP audit entries, including:

* tool name
* accountId
* success/error flag
* error codes
* error messages
* duration
* timestamp

**Example:**

```
curl -s "http://localhost:8080/audit/recent?limit=10" | jq .
```

**Sample output:**

```
[
  {
    "id": 33,
    "rpcMethod": "tools/call",
    "toolName": "search_tweets",
    "accountId": "test-account",
    "ok": true,
    "durationMs": 1189,
    "errorCode": null,
    "errorMessage": null,
    "createdAt": "2025-11-16T08:54:10.716Z"
  }
]
```

This fills the gap that existed in Iteration 1, where real-time audit inspection was missing.

---

### **2. `/audit/summary` — Tool-level metrics (NEW)**

**Endpoint:**

```
GET /audit/summary?hours=<H>
```

Provides aggregated monitoring statistics per tool:

* total calls
* ok calls
* error calls
* average execution duration
* last call timestamp

**Example:**

```
curl -s "http://localhost:8080/audit/summary?hours=24" | jq .
```

**Output:**

```
[
  {
    "toolName": "search_tweets",
    "totalCalls": 2,
    "okCalls": 2,
    "errorCalls": 0,
    "avgDurationMs": 6123,
    "lastCallAt": "2025-11-16T08:54:10.716Z"
  }
]
```

This aligns with Iteration-2 expectations for lightweight dashboard-style metrics.

---

## **How to Demo the Enhancements**

---

### **Step 1 — Trigger MCP calls**

**Successful tool call:**

```
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{
        "jsonrpc":"2.0",
        "id":1,
        "method":"tools/call",
        "params":{
          "name":"echo_test",
          "arguments":{"text":"demo"}
        }
      }' | jq .
```

**Real Mastodon-backed search:**

```
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{
        "jsonrpc":"2.0",
        "id":2,
        "method":"tools/call",
        "params":{
          "name":"search_tweets",
          "arguments":{
            "accountId":"test-account",
            "q":"hello",
            "limit":5
          }
        }
      }' | jq .
```

**Force an error:**

```
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{
        "jsonrpc":"2.0",
        "id":3,
        "method":"tools/call",
        "params":{
          "name":"no_such_tool",
          "arguments":{}
        }
      }' | jq .
```

---

### **Step 2 — Fetch recent audit entries**

```
curl -s "http://localhost:8080/audit/recent?limit=10" | jq .
```

---

### **Step 3 — Get per-tool usage metrics**

```
curl -s "http://localhost:8080/audit/summary?hours=24" | jq .
```

---

## **How it is wired internally**

---

### **A. MCP → AuditService**

Every `/mcp` call triggers:

```
audit.save(method, toolName, accountId, ok, duration, errorCode, errorMessage);
```

This consistently records:

* success cases
* invalid parameters
* unknown tools
* internal errors

Every call is audited without exception.

---

### **B. HTTP request logging**

Every HTTP request is logged in the format:

```
http GET /search -> 200 (12 ms)
```

via:

```
WebRequestLogFilter
```

This includes routes such as `/search`, `/auth/*`, `/analytics/*`, `/mcp`, `/schedule_tweet`, `/audit/*`, and others.

---

## **Database Schema (tool_call_audit)**

| Column        | Type         | Description           |
| ------------- | ------------ | --------------------- |
| id            | bigint (PK)  | Auto-generated        |
| rpc_method    | varchar(40)  | Always "tools/call"   |
| tool_name     | varchar(64)  | Tool invoked          |
| account_id    | varchar(64)  | Optional              |
| ok            | boolean      | Success flag          |
| duration_ms   | bigint       | Execution time        |
| error_code    | int          | JSON-RPC error code   |
| error_message | varchar(255) | Trimmed error message |
| created_at    | timestamp    | Insert time           |

The schema is created automatically through our Flyway migrations (`V3__audit.sql`).

---

## **What changed from Iteration 1 → Iteration 2**

### **Completed in Iteration 2**

* Added `/audit/recent` endpoint
* Added `/audit/summary` endpoint
* Full auditing for both success and error paths
* Tool-level duration, counts, and timestamps
* Integrated with real Mastodon-backed tools
* Includes HTTP-level logging

---

### **Not required (and intentionally excluded)**

* Correlation IDs
* Full payload redaction or PII classification
* Distributed tracing
* Real-time streaming dashboards

---

## **Final Status – Audit System**

The audit subsystem now provides a comprehensive and dependable set of observability features that align with Iteration-2 objectives.
It supports ongoing debugging, performance monitoring, and transparency across all MCP tools and REST endpoints.

