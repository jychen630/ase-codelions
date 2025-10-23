# MCP Twitter Management Platform

### Iteration 1 – Columbia University ASE Project

## Overview

This project implements a modular **Model Context Protocol (MCP) server** that simulates a scalable backend for managing and analyzing Twitter-like data.
It supports authentication, search, scheduling, analytics, and auditing — all exposed through a JSON-RPC 2.0 MCP interface (`/mcp`).

The system is built with **Spring Boot**, uses **Flyway** for database schema migrations, and relies on **H2 (file-mode)** for persistent development storage (PostgreSQL-compatible).

---

## Key Features

### 1. Authentication & Token Management

* Implements secure token storage using `DbTokenStore`, `TokenCredentialRepository`, and `SecretCryptoService`.
* Simulated OAuth 2.0 flow through `/auth/start` and `/auth/callback`.
* Supports multiple tenants (logical accounts).

### 2. Search & Discovery

* Full-text-lite keyword and hashtag search over stored tweets.
* `SearchService` and `SearchController` support phrase, keyword, and hashtag queries.
* Backed by H2 (or PostgreSQL) with persistent tweets stored in `tweets` table.

### 3. Scheduling Framework

* Tweets can be scheduled for future posting.
* Core classes: `SchedulingService`, `SchedulerRunner`, and `ScheduledPostRepository`.
* DB-backed and persists across restarts.
* Integrated MCP tool: `/tools/schedule_tweet`.

### 4. Analytics Engine

* Provides top hashtags, posting-hour analytics, and timeline summaries.
* Backed by `AnalyticsService` and `AnalyticsController`.
* Works on both `timeline` and `db` sources.

### 5. Audit & Logging

* Every MCP tool call is recorded in `tool_call_audit` with timing, status, and error codes.
* `AuditService`, `ToolCallAudit`, and `WebRequestLogFilter` implement the persistence and logging logic.

### 6. MCP Core & Tools

* Central `/mcp` endpoint handling `initialize`, `tools/list`, and `tools/call`.
* Tools include:

  * `echo_test`
  * `check_quota_status`
  * `get_token` / `set_token` / `list_tokens`
  * `search_tweets`
  * `schedule_tweet`

### 7. Persistent DB & Config

* H2 File-mode DB configured in `application-devdb.properties`
  (`jdbc:h2:file:./.h2/mcp;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`)
* Flyway migrations handle schema versioning (`V1__init.sql`, `V2__tweets.sql`, `V3__audit.sql`).

---

## Quality and Testing

| Metric                    | Result                     |
| ------------------------- | -------------------------- |
| **Line Coverage**         | ~74%                       |
| **Branch Coverage**       | ~62%                       |
| **Total Classes Tested**  | 55                         |
| **Style Checking**        | Checkstyle (default ruleset: sun_checks.xml)                         |
| **Static Analysis Tools** | PMD (default Java rulset)            |
| **Test Framework**        | JUnit 5 + Spring Boot Test |
| **Build Tool**            | Maven 3.9+                 |

**JaCoCo Report Summary:**

* Most modules exceed 70% coverage.
* Highest: `tools`, `auth.web`, `timeline`, and `security` (90–100%).
* Core MCP package: 72%.
* All critical logic paths covered in search, analytics, and scheduling.

To generate reports:
```bash
mvn checkstyle:check
mvn pmd:pmd
```
We have included checkstyle and pmd reports for our latest tagged release (v1.0) under target/site/ in this repo.

Test configuration files are under src/test in this repo.

---

## How to Run

### 1. Build & Run

```bash
mvn clean package
mvn -q spring-boot:run -Dspring-boot.run.profiles=devdb \
  -Dspring-boot.run.jvmArguments="-Dapp.search.source=db"
```

### 2. H2 Console

Open [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

**JDBC URL:** `jdbc:h2:file:./.h2/mcp;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
**User:** `sa`
**Password:** (leave blank)

### 3. Example MCP Calls

**Echo Test**

```bash
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/call",
       "params":{"name":"echo_test","arguments":{"text":"hello"}}}'
```

**Search Example**

```bash
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call",
       "params":{"name":"search_tweets","arguments":{"accountId":"acctA","q":"hello","limit":5}}}'
```

**Schedule Tweet**

```bash
FUTURE=$(date -u -d '+1 minute' --iso-8601=seconds)
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",
       \"params\":{\"name\":\"schedule_tweet\",\"arguments\":{\"text\":\"demo tweet\",\"time\":\"${FUTURE}\"}}}"
```

---

## Directory Structure

```
src/main/java/com/team/mcp/
│
├── analytics/        → AnalyticsService, AnalyticsController
├── audit/            → ToolCallAudit, AuditService, WebRequestLogFilter
├── auth/             → OAuth flow, Token management
│   └── web/          → AuthController
├── config/           → Flyway, Clock, Scheduling configuration
├── mcp/              → Core JSON-RPC tools and controllers
│   └── dto/          → McpRequest / McpResponse
├── scheduling/       → SchedulingService, ScheduledPost, SchedulerRunner
├── search/           → SearchService, TweetEntity, SearchController
├── security/         → TokenProvider utilities
├── timeline/         → TimelineService
└── twitter/          → TwitterClient, FakeTwitterClient, Tweet DTO
```

---

## API Documentation

Each functional module includes its own detailed API specification covering
available endpoints, expected inputs/outputs, error/status codes, and any
call-order constraints.  

Use the links below to navigate directly to the documentation for each module:

| Module | Description | API Documentation |
|---------|--------------|-------------------|
| **MCP Core** | Main JSON-RPC 2.0 `/mcp` interface and tool registry | [MCP API Docs](./src/main/java/com/team/mcp/mcp/Mcp.md) |
| **Authentication** | OAuth-style token flow and credential management | [Auth API Docs](./src/main/java/com/team/mcp/auth/Audit.md) |
| **Search & Discovery** | Keyword and hashtag search endpoints | [Search API Docs](./src/main/java/com/team/mcp/search/SearchAndDiscovery.md) |
| **Scheduling** | Tweet scheduling and delayed posting | [Scheduling API Docs](./src/main/java/com/team/mcp/scheduling/Scheduling.md) |
| **Analytics** | Timeline and posting analytics endpoints | [Analytics API Docs](./src/main/java/com/team/mcp/analytics/Analytics.md) |
| **Audit & Logging** | Audit trail for tool calls and request logs | [Audit API Docs](./src/main/java/com/team/mcp/audit/Audit.md) |

Each linked file documents:
- Endpoint paths and HTTP/JSON-RPC methods  
- Input parameters and expected data schemas  
- Output objects and example responses  
- Error/status codes (e.g. 200, 400, 404, 500)  
- Any call-order dependencies or restrictions

---

## Iteration 1 Deliverables

1. Functional MCP JSON-RPC server
2. DB-backed token management and OAuth simulation
3. Full search, analytics, and scheduling flows
4. Persistent audit logging for all tool calls
5. Flyway-managed schema with three stable migrations
6. Code coverage above 55% (JaCoCo)
7. Passing static analysis checks (PMD, SpotBugs, Checkstyle)

---

## Next Steps (Iteration 2)

* Replace FakeTwitterClient with live Twitter API integration.
* Expand analytics to live engagement data.
* Implement real posting and interaction endpoints.
* Add Redis caching and rate limiting.
* Build visualization dashboard for aggregated analytics.

---

## License

For academic use only — Columbia University ASE Team Project.
All rights reserved © 2025.

---
