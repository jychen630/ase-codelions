# **MCP Server – Iteration 2 Documentation**

---

## **1. Overview**

This project implements a Multi-Client Platform (MCP) backend that automates social media tasks for a Mastodon account, including:

* OAuth-based authentication
* Timeline search (keywords, boolean operators, hashtags)
* Scheduling posts
* Analytics (sentiment, top hours, summary)
* Audit logging of tool calls
* MCP / JSON-RPC tool layer
* A Python natural-language (NL) client that drives the system

---

## **1.1 Why Mastodon instead of Twitter**

The original concept involved Twitter. However, current Twitter Developer API pricing makes realistic experimentation difficult. Posting and search capabilities exist only in paid tiers costing approximately $100–$200 per month, which is not feasible for coursework.

Mastodon provides:

* an open REST + OAuth API
* the ability to post and read timelines at no cost
* behavior similar enough to Twitter for the project’s goals

For these reasons, Iteration 2 uses Mastodon (mastodon.social) while retaining the same architecture and functional plan.

---

## **1.2 Why a custom NL client instead of a hosted AI**

The specification expects an “AI-style” client capable of interpreting natural language and calling MCP tools.

Rather than relying on a paid hosted AI platform (which could introduce costs, lock-in, and external dependencies), Iteration 2 provides a local Python-based NL interpreter:

* implemented in pure Python, without external AI APIs
* compatible with Linux, macOS, and Windows
* converts natural language into CLI arguments and HTTP requests
* drives all major MCP features (authentication, search, analytics, scheduling, audit)

This keeps the solution free, portable, and easy to evaluate.

---

# **2. How to Build and Run**

---

## **2.1 Prerequisites**

* Java 21
* Maven 3.9+
* Python 3.8+ (for the NL client)
* `curl` and `jq` (recommended)

**Project layout (relevant parts):**

```
FinalProjectTeam/
├─ src/main/java/...          # Spring Boot MCP server
├─ src/test/java/...          # Unit + integration tests
├─ src/main/resources/
│   ├─ application.properties
│   └─ application-mastodon.properties
├─ Client/
│   └─ mcp_cli.py             # Python NL + CLI client
└─ pom.xml
```

---

## **2.2 Environment variables (local)**

For local development in timeline mode:

```
export APP_KMS_KEY=dev-local-key
export APP_SEARCH_SOURCE=timeline
```

In the submitted version, Mastodon client credentials are already wired into `TwitterOAuthClient` for convenience, making additional OAuth variables optional for local runs.

---

## **2.3 Run locally – timeline (Mastodon) mode**

From the project root:

```
mvn -q spring-boot:run
```

By default, the server:

* listens on `http://localhost:8080`
* uses the Mastodon timeline as the search source
* uses file-based H2 for tokens, audit, and scheduling

---

## **2.4 Run locally – DB mode (Iteration-1 style)**

```
mvn -q spring-boot:run \
 -Dspring-boot.run.arguments="--app.search.source=db"
```

This uses the `TWEETS` table in the embedded H2 database, seeded via Flyway migrations.

---

## **2.5 Python NL client configuration**

From the `Client/` directory:

```
cd Client
export MCP_BASE_URL='http://localhost:8080'
```

For the deployed cloud instance:

```
export MCP_BASE_URL='https://mcp-iteration2-production.up.railway.app'
```

Run commands such as:

```
python3 mcp_cli.py nl "show sentiment analysis for account test-account"
```

---

# **3. Cloud Deployment and Tagging (Iteration 2)**

---

## **3.1 Cloud deployment (Railway)**

The backend is deployed to:

```
https://mcp-iteration2-production.up.railway.app
```

This instance runs:

* the same Spring Boot MCP server
* persistent H2 file DB in the container
* `app.search.source=timeline`
* the configured Mastodon OAuth client
* `/auth/callback` exposed at:

```
https://mcp-iteration2-production.up.railway.app/auth/callback
```

### **How the grader can use the cloud deployment**

Inspect metadata:

```
https://mcp-iteration2-production.up.railway.app/auth/meta
```

Start OAuth login:

```
curl -s "https://mcp-iteration2-production.up.railway.app/auth/start?accountId=test-account" | jq
```

Opening the returned `authorize_url` in a browser authenticates with Mastodon.
After approval, the token is stored in the Railway DB, enabling all endpoints to operate on live data.

To point the NL client at the cloud instance:

```
cd Client
export MCP_BASE_URL='https://mcp-iteration2-production.up.railway.app'
```

Example:

```
python3 mcp_cli.py nl "show sentiment analysis"
python3 mcp_cli.py nl "schedule a tweet in 2 minutes saying good morning"
```

---

## **3.2 Git tag for Iteration 2**

Tag:

```
iteration-2-mcp
```

The tagged commit:

* contains all Iteration-2 features
* passes `mvn verify`
* includes documentation of the Railway deployment URL

---

# **4. API Overview**

Base URLs:

```
Local: http://localhost:8080
Cloud: https://mcp-iteration2-production.up.railway.app
```

Logical test account:

```
test-account
```

---

## **4.1 Authentication**

### **GET /auth/meta**

Returns provider metadata.

### **GET /auth/start?accountId={id}**

Starts OAuth; returns the authorize URL, state value, and callback.

### **GET /auth/callback?code=...&state=...**

Processes the OAuth return value and stores the encrypted token.

### **GET /auth/status?accountId={id}**

Returns token state, provider, scopes, expiry, and health.

### **DELETE /auth/token?accountId={id}**

Deletes the stored token.

---

## **4.2 Search and Hashtags**

Search operates either on:

* the Mastodon home timeline (default), or
* the H2 database (db mode)

### **GET /search?accountId={id}&q={query}&limit={n}&offset={k}**

Features:

* keyword search
* boolean OR
* simple quoted phrases
* pagination
* ranking by match score and recency

Examples:

```
curl -s "$BASE/search?accountId=$ACCOUNT&q=hello&limit=5" | jq
curl -s "$BASE/search?accountId=$ACCOUNT&q=hello%20OR%20second&limit=5" | jq
```

### **GET /search/hashtags?accountId={id}&q=#tag&limit={n}**

Exact hashtag match (case-insensitive).

---

## **4.3 Analytics**

Analytics run over the timeline or DB, depending on configuration.

### **GET /analytics/sentiment?accountId={id}**

Returns sentiment counts and average score.

### **GET /analytics/best-hours?accountId={id}**

Returns a mapping of hour → number of posts.

### **GET /analytics/top-hashtags?accountId={id}&n={k}**

Returns the top-k hashtags.

### **GET /analytics/summary?accountId={id}**

Includes totals, top hashtags, and the best posting hour.

---

## **4.4 Scheduling**

### **POST /tools/schedule_tweet**

Example body:

```
{
  "tool": "schedule_tweet",
  "params": {
    "accountId": "test-account",
    "text": "Hello from Railway!",
    "time": "2025-12-03T07:56:14Z"
  }
}
```

The job is stored and later executed by the background scheduler.

The NL client can schedule posts as well:

```
python3 mcp_cli.py nl "schedule a tweet in 2 minutes saying good morning"
```

---

## **4.5 Audit**

### **GET /audit/recent?limit={n}**

Returns recent audit rows.

### **GET /audit/summary?hours={h}**

Aggregated statistics per tool.

---

## **4.6 MCP / JSON-RPC**

### **POST /mcp**

Implements JSON-RPC with:

* `tools/list`
* `tools/call`

List tools:

```
curl -s "$BASE/mcp" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' | jq
```

Tools include:

* set_token
* get_token
* list_tokens
* search_tweets
* check_quota_status
* echo_test

Errors follow the JSON-RPC specification.

---

# **5. Natural-Language Client**

Location:

```
Client/mcp_cli.py
```

---

## **5.1 Modes**

* direct subcommands
* `nl` mode for natural-language interpretation

---

## **5.2 Configuration**

```
cd Client
export MCP_BASE_URL='http://localhost:8080'
# or:
export MCP_BASE_URL='https://mcp-iteration2-production.up.railway.app'
```

---

## **5.3 Example NL commands**

```
python3 mcp_cli.py nl "log in account test-account"
python3 mcp_cli.py nl "show sentiment analysis"
python3 mcp_cli.py nl "show analytics summary"
python3 mcp_cli.py nl "what are the best hours to post?"
python3 mcp_cli.py nl "schedule a tweet in 2 minutes saying good morning"
python3 mcp_cli.py nl "show recent audit entries"
```

The client prints both:

* the interpreted CLI command
* the MCP JSON response

The NL interpreter handles intent detection, numeric phrase parsing, and extraction of quoted or contextual text.

---

# **6. Testing and Coverage**

---

## **6.1 Test types**

Unit tests cover:

* search ranking and parsing
* analytics logic
* OAuth client behavior
* MCP tools

Integration tests cover:

* REST controllers
* OAuth flow components
* scheduling endpoint shape
* audit endpoints

---

## **6.2 Equivalence partitions and boundary tests**

Coverage includes edge cases for:

* search queries (single term, multi-term, OR, phrases)
* offsets and limits
* hashtags (valid and invalid)
* analytics (empty vs. non-empty sets)
* scheduling (valid times, malformed timestamps)
* OAuth responses and errors

---

## **6.3 Coverage**

Measured with JaCoCo.

Branch coverage:

```
≈81%
```

Generated reports:

```
target/site/jacoco/index.html
```

Run locally:

```
mvn -q verify
```

---

## 7. End-to-end Client/Service Test Checklist

The following manual tests exercise the client and service together. All tests assume:

- Service is running (locally or on Railway).
- MCP_BASE_URL is set accordingly.
- Logical account id is test-account.

1. *Login flow (NL client + OAuth)*
   - Command:
     ```
     python3 Client/mcp_cli.py nl "log in account test-account"
     ```     
   - Expected: Client prints an authorize_url; opening it in a browser allows Mastodon login; afterwards GET /auth/status shows hasToken=true.

2. *Sentiment analytics via NL*
   - Command:
   ```
   python3 Client/mcp_cli.py nl "show sentiment analysis"
   ```
   - Expected: JSON with totalTweets > 0 and non-trivial sentiment counts.

3. *Analytics summary via NL*
   - Command:
   ```
   python3 Client/mcp_cli.py nl "show analytics summary"
   ```
   - Expected: JSON with totalTweets, topHashtags (possibly empty), and bestHourUtc.

4. *Best posting hours via NL*
   - Command:
   ```
   python3 Client/mcp_cli.py nl "what are the best hours to post?"
   ```
   - Expected: Map from hours to counts.

5. *Schedule a post via NL*
   - Command:
   ```
   python3 Client/mcp_cli.py nl "schedule a tweet in 2 minutes saying good morning"
   ```
   - Expected: JSON with "status": "scheduled" and a future scheduled_for timestamp. Shortly after that time, the post appears on Mastodon.

6. *Audit via NL*
   - Command:
   ```
   python3 Client/mcp_cli.py nl "show recent audit entries"
   ```
   - Expected: List of recent tool calls (may be empty initially, then fill as tests run)
---

# **8. Static Analysis and CI**

---

## **8.1 CI setup**

GitHub Actions workflow:

```
.github/workflows/java-ci.yml
```

Triggers:

* pushes to `main`
* pull requests to `main`
* pushes to the Iteration-2 branch

Job runs:

```
mvn -B verify
```

Which includes tests, Checkstyle, PMD, SpotBugs, and coverage.

---

## **8.2 Static analysis summary**

Before refactoring:

* PMD: 1 violation
* Checkstyle: formatting and missing Javadoc
* SpotBugs: no high-priority issues

After adjustments:

* PMD: 0 violations
* Checkstyle: 0 violations
* SpotBugs: 0 high/medium issues

Reports:

```
target/pmd.xml
target/checkstyle-result.xml
target/spotbugsXml.xml
target/surefire-reports/
target/site/jacoco/index.html
```

---

# **9. DB Mode (Iteration-1 Compatibility)**

DB-backed search remains available for:

* predictable, reproducible datasets
* offline testing
* environments without Mastodon connectivity

---

## **9.1 Run in DB mode**

```
mvn -q spring-boot:run \
 -Dspring-boot.run.arguments="--app.search.source=db"
```

---

## **9.2 Example DB search commands**

```
BASE='http://localhost:8080'
ACCOUNT='test-account'
```

**Keyword search:**

```
curl -s "$BASE/search?accountId=$ACCOUNT&q=hello&limit=10" | jq
```

**Hashtag search:**

```
curl -s "$BASE/search/hashtags?accountId=$ACCOUNT&q=%23db&limit=10" | jq
```

**Broad search example:**

```
curl -s "$BASE/search?accountId=$ACCOUNT&q=a&limit=20" | jq
```

If no match exists, responses simply return:

```
[]
```

The embedded H2 database avoids the need for any external database.

The cloud instance defaults to timeline mode, but DB mode remains useful for deterministic local tests.



---
