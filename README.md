# **Iteration 2 — Final Version**

---

## **1. Overview**

This project implements the final version of our social-media automation service.
Iteration 2 transforms the prototype from Iteration 1 into a fully functional, cloud-deployed system with:

* Real Mastodon OAuth 2.0 authentication
* Real posting and real searching against the user’s Mastodon timeline
* Scheduling with background execution
* Analytics (sentiment, hashtag extraction, best posting hours)
* Full auditing subsystem
* MCP tools over JSON-RPC
* Natural-language client that drives all features
* Continuous Integration + Static Analysis (Checkstyle, PMD, SpotBugs)
* Automated + manual end-to-end tests
* Deployment on Railway (cloud URL submitted for grading)

All code and tests live in the main branch as required.

---

## **2. What Changed from Iteration 1**

Iteration 1 used mock data and fake authentication.

Iteration 2 includes major upgrades:

| Feature    | Iteration 1     | Iteration 2                                       |
| ---------- | --------------- | ------------------------------------------------- |
| OAuth      | Mock            | Real Mastodon OAuth + token storage               |
| Posting    | Local no-op     | Real Mastodon API posting                         |
| Search     | In-memory mock  | Live Mastodon timeline search (AND/OR + hashtags) |
| Scheduling | No real posting | Background posting to Mastodon                    |
| Analytics  | Mock counters   | Real ML sentiment, real best hours                |
| Audit      | Basic logging   | Full audit API + summaries                        |
| Client     | Only REST       | NLP client (AI-like) + MCP                        |
| Deployment | Local only      | Railway cloud deployment                          |

No required feature was omitted.

---

## **3. How to Build and Run Locally**

### Requirements

* Java 17
* Maven
* Python 3.9+ (for the NLP client)

### Start the service (default: live Mastodon timeline mode)

```
mvn -q spring-boot:run
```

### Run using DB mode (Iteration 1 style)

```
mvn -q spring-boot:run   -Dspring-boot.run.profiles=devdb   -Dspring-boot.run.arguments="--app.fakeTwitter=false --app.search.source=db"
```

### Run the NLP Client

```
cd Client
export MCP_BASE_URL='http://localhost:8080'
python3 mcp_cli.py nl "show sentiment analysis"
```

---

## **4. Cloud Deployment (Required Submission URL)**

The service is deployed on Railway:

[https://mcp-iteration2-production.up.railway.app](https://mcp-iteration2-production.up.railway.app)

This URL was submitted as the assignment deliverable.

The deployment includes:

* Real OAuth callback
* Background scheduler
* Mastodon integration
* All required endpoints

No secrets are stored in the repository; Railway environment variables provide OAuth credentials.

---

## **5. End-to-End Client Testing (Required by Spec)**

End-to-end tests are run using our natural-language (NL) client, which exercises the full pipeline including OAuth, posting, search, analytics, audit, and MCP tools.

### Set API base URL:

```
cd Client
export MCP_BASE_URL='https://mcp-iteration2-production.up.railway.app'
```

### ✔ Login

```
python3 mcp_cli.py nl "log in account test-account"
```

### ✔ Search

```
python3 mcp_cli.py nl "search for a text saying hello"
```

### ✔ Schedule a post

```
python3 mcp_cli.py nl "schedule a tweet in 2 minutes saying good morning"
```

### ✔ Analytics

```
python3 mcp_cli.py nl "show analytics summary"
python3 mcp_cli.py nl "show sentiment analysis"
python3 mcp_cli.py nl "what are the best hours to post?"
```

### ✔ Audit

```
python3 mcp_cli.py nl "show recent audit entries"
```

### ✔ MCP tools

```
curl -s "$BASE/mcp" \
 -H "Content-Type: application/json" \
 -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' | jq
```

A checklist of test steps is provided so graders can re-run the same tests.

---

## **6. Static Analysis (Required by Spec)**

Static analysis tools used:

* Checkstyle
* PMD
* SpotBugs

All tools run automatically as part of CI (mvn verify).

### Initial Results

* PMD: 1 violation (UselessParentheses)
* Checkstyle: several formatting & Javadoc issues
* SpotBugs: no high-priority issues

### Final Results (After Fixes)

* PMD: 0 violations
* Checkstyle: 0 violations
* SpotBugs: 0 high/medium issues

### Report Locations (Generated automatically by Maven):

| Tool       | Path                          |
| ---------- | ----------------------------- |
| PMD        | target/pmd.xml                |
| Checkstyle | target/checkstyle-result.xml  |
| SpotBugs   | target/spotbugsXml.xml        |
| Tests      | target/surefire-reports/      |
| Coverage   | target/site/jacoco/index.html |

---

## **7. Continuous Integration (Required by Spec)**

CI is implemented using GitHub Actions.

Workflow file:
`.github/workflows/java-ci.yml`

Triggered on:

* Every push to main
* Every PR targeting main
* Iteration-2 feature branch development

The workflow executes:

```
mvn -B verify
```

This runs:

* Unit tests
* Integration tests
* Static analysis (Checkstyle, PMD, SpotBugs)
* Code coverage (JaCoCo)

All CI runs are visible in the repository’s Actions tab.

---

## **8. Testing Summary (Required)**

### Unit Tests

Coverage measured with JaCoCo:

**81% branch coverage** (above the typical requirement)

### Integration Tests cover:

* OAuth start/callback flow
* Searching (keyword, OR, hashtag)
* Scheduling pipeline end-to-end
* Posting to Mastodon (mocked + real)
* Analytics
* Audit logging

All tests pass in CI.

---

## **9. Bug Finding (Required by Spec)**

Static analysis tools identified:

* Parentheses misuse (PMD)
* Missing Javadoc / long lines (Checkstyle)

All bugs have been fixed.

SpotBugs did not report any high-priority defects.

---

## **10. DB Search Mode (From Iteration 1) Still Supported**

The service supports a local H2 database mode, useful for testing without Mastodon.

### Run DB mode:

```
mvn -q spring-boot:run \
 -Dspring-boot.run.arguments="--app.search.source=db"
```

### Example commands:

```
BASE='http://localhost:8080'
ACCOUNT='test-account'

curl -s "$BASE/search?accountId=$ACCOUNT&q=hello&limit=10" | jq
curl -s "$BASE/search/hashtags?accountId=$ACCOUNT&q=%23db&limit=10" | jq
curl -s "$BASE/search?accountId=$ACCOUNT&q=a&limit=20" | jq
```

H2 was chosen so graders do not need PostgreSQL installed.

---

## **11. Deployment & Tagging (Required by Spec)**

The final version of Iteration 2 is tagged:

`iteration-2-final`

Cloud deployment:

[https://mcp-iteration2-production.up.railway.app](https://mcp-iteration2-production.up.railway.app)

This is the URL submitted for grading.


