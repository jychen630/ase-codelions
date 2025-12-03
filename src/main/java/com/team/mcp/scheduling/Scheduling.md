# **Scheduling — Iteration 2**

---

## **Real-Time Post Scheduling with Mastodon Integration and MCP Support**

---

## **1. Overview**

The Scheduling subsystem enables delayed posting of Mastodon messages using:

* a REST endpoint (`/tools/schedule_tweet`)
* a background scheduler that runs periodically
* a fully validated MCP tool (`schedule_tweet`)
* real Mastodon API posting using the authenticated user token
* full database persistence (`scheduled_posts` table)
* error tracking and audit logging

In Iteration 1, scheduled entries were stored but not published.
Iteration 2 now performs real Mastodon posting through the user’s OAuth token and Mastodon’s:

```
/api/v1/statuses
```

endpoint.

---

## **2. Architecture**

Scheduling consists of four cooperating components.

---

### **a. REST wrapper (`/tools/schedule_tweet`)**

Accepts scheduling requests containing:

* accountId
* text
* time (ISO-8601, UTC format)

---

### **b. MCP tool (`schedule_tweet`)**

Provides identical behavior via JSON-RPC:

```
{
  "method": "tools/call",
  "params": {
    "name": "schedule_tweet",
    "arguments": {
      "accountId": "test",
      "text": "Hello!",
      "time": "2025-11-16T19:45:00Z"
    }
  }
}
```

---

### **c. Database layer**

The `schedule_posts` table contains:

| Column          | Meaning                         |
| --------------- | ------------------------------- |
| id              | primary key                     |
| account_id      | logical tenant                  |
| text            | post content                    |
| run_at          | scheduled time (UTC)            |
| status          | `PENDING` / `POSTED` / `FAILED` |
| posted_tweet_id | Mastodon returned ID            |
| created_at      | audit timestamp                 |

---

### **d. Background Scheduler Job**

Runs every few seconds and:

* finds posts where `run_at <= now` and `status = 'PENDING'`
* posts content using `MastodonClient`
* updates status to `POSTED` or `FAILED`
* stores `posted_tweet_id`
* emits an audit entry

---

## **3. REST API**

### **POST `/tools/schedule_tweet`**

**Request:**

```
{
  "tool": "schedule_tweet",
  "params": {
    "accountId": "test-account",
    "text": "Hello from scheduler!",
    "time": "2025-11-15T20:50:00Z"
  }
}
```

**Response:**

```
{
  "id": "135",
  "status": "scheduled",
  "scheduled_for": "2025-11-15T20:50:00Z",
  "text": "Hello from scheduler!"
}
```

Possible errors include:

* missing fields
* empty text
* invalid timestamp
* account without an OAuth token

---

## **4. MCP Tool Definition**

**Tool:** `schedule_tweet`

**Arguments:**

| Name      | Type   | Required | Description                   |
| --------- | ------ | -------- | ----------------------------- |
| accountId | string | yes      | logical tenant / user account |
| text      | string | yes      | content to post               |
| time      | string | yes      | ISO-8601 UTC timestamp        |

**Example MCP call:**

```
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "schedule_tweet",
    "arguments": {
      "accountId": "test-account",
      "text": "Test post",
      "time": "2025-11-15T21:00:00Z"
    }
  }
}
```

---

## **5. Iteration 2 Improvements**

Iteration 1 → 2 enhancements include:

### **Real Mastodon Posting**

Posts are now sent using:

```
POST https://mastodon.social/api/v1/statuses
Authorization: Bearer <access token>
```

### **True Background Execution**

The scheduler processes posts dynamically rather than via mocked logic.

### **Status Tracking**

Each scheduled job now maintains one of:

* `PENDING`
* `POSTED`
* `FAILED`

### **Audit Integration**

Each execution records an audit entry via `AuditService`.

### **Natural Language Support**

Scheduling can be performed through the Python NL client:

```
python3 mcp_cli.py nl "schedule a tweet in 2 minutes saying good morning"
```

### **Error Handling**

Handles:

* missing token
* invalid timestamp
* Mastodon API errors
* network failures

Failures appear via:

```
/audit/recent
```

---

## **6. Testing the System**

### **Step 1 — OAuth Login**

```
curl -s "http://localhost:8080/auth/start?accountId=test" | jq
```

Open the `authorize_url` in a browser and approve access.

---

### **Step 2 — Schedule a post**

```
FUTURE=$(date -u -d '+60 seconds' --iso-8601=seconds)

curl -s http://localhost:8080/tools/schedule_tweet \
  -H 'Content-Type: application/json' \
  -d "{\"tool\":\"schedule_tweet\",\"params\":{\"accountId\":\"test\",\"text\":\"Hello world\",\"time\":\"${FUTURE}\"}}" | jq
```

---

### **Step 3 — Watch it post**

Within roughly one minute:

```
curl -s "http://localhost:8080/analytics/summary?accountId=test" | jq
```

or inspect the Mastodon client.

---

### **Step 4 — Verify audit**

```
curl -s "http://localhost:8080/audit/recent?limit=5" | jq
```

---

## **7. Natural Language Examples**

Using the Python CLI:

```
python3 mcp_cli.py nl "schedule a tweet in 2 minutes saying hello world"
python3 mcp_cli.py nl "schedule a tweet in 30 seconds saying testing the scheduler"
```

---

## **8. Summary**

The scheduling system is now:

* stable
* real
* multi-tenant
* audited
* compatible with analytics and ML features
* accessible via REST, MCP, and natural language

