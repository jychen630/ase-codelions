# **Equivalence Partitions & Boundary Testing**

This document describes the equivalence partitions and boundary cases used when designing tests for the MCP server and its main features in Iteration 2.

It covers:

* Authentication (Mastodon OAuth)
* Search (/search)
* Hashtag search (/search/hashtags)
* Analytics (/analytics/*)
* Scheduling (/tools/schedule_tweet)
* Audit (/audit/*)
* MCP tools (/mcp)
* DB vs timeline search source
* Natural-language (NL) client behavior (at a high level)

Where possible, partitions are exercised both at the service layer (unit tests) and at the HTTP/controller level (integration tests).

---

# **1. Authentication (OAuth) – Equivalence Partitions**

## **1.1 /auth/start?accountId=...**

**Dimensions:**

* accountId format
* Token existence / state

**Input dimension table**

| Input dimension | Partition                                         | Expected behavior                                            | Coverage type          |
| --------------- | ------------------------------------------------- | ------------------------------------------------------------ | ---------------------- |
| accountId       | Non-empty, well-formed string (e.g. test-account) | Returns authorize_url, state, and callback.                  | Controller + NL client |
| accountId       | Empty / missing                                   | 4xx error (validation failure or rejected request).          | Controller             |
| Token existence | No token stored yet                               | auth/start still returns an authorize URL (no special case). | Controller             |
| Token existence | Token already stored                              | auth/start still returns an authorize URL (re-auth allowed). | Controller             |

Boundary: accountId with special characters (e.g. dashes, underscores) is treated as valid.

---

## **1.2 /auth/status?accountId=...**

**Dimensions:**

* Token stored vs not stored
* Token health

**Input dimension table**

| Input dimension | Partition                        | Expected behavior                                                | Coverage type       |
| --------------- | -------------------------------- | ---------------------------------------------------------------- | ------------------- |
| Token presence  | No token for that account        | hasToken=false, health="missing" or equivalent “no token” state. | Controller/service  |
| Token presence  | Valid, non-expired token stored  | hasToken=true, provider/scopes set, health="healthy".            | Controller/service  |
| Token health    | Expired token / invalid metadata | hasToken=true, health="expired" or degraded state.               | Service-level logic |

---

## **1.3 TwitterOAuthClient.exchangeWithMetadata(...)**

This is exercised via unit tests with a mocked HttpClient.

**Dimensions:**

* HTTP status from /oauth/token
* Body content
* IO failures

**Input dimension table**

| Input dimension  | Partition                        | Expected behavior                                        | Coverage type        |
| ---------------- | -------------------------------- | -------------------------------------------------------- | -------------------- |
| HTTP status code | 2xx                              | Access token parsed, OAuthTokens returned successfully.  | Unit (exchange test) |
| HTTP status code | Non-2xx (e.g. 500)               | RuntimeException thrown, wrapping IllegalStateException. | Unit                 |
| Body content     | Contains "access_token" field    | Parsed successfully.                                     | Unit                 |
| Body content     | Missing "access_token" field     | RuntimeException wrapping IllegalStateException.         | Unit                 |
| HTTP client      | HttpClient.send throws exception | RuntimeException wrapping original IOException.          | Unit                 |

Boundaries: very short response bodies, unexpected JSON format.

---

# **2. Search – /search**

Search operates over two data sources:

* Mastodon timeline (app.search.source=timeline)
* Local H2 DB (app.search.source=db)

The same endpoint is used; the partitions below apply to both modes.

## **2.1 Query structure (q)**

**Dimensions:**

* Empty vs non-empty
* Terms vs OR expressions
* Phrase vs plain

**Input dimension table**

| Dimension       | Partition                                   | Expected behavior                                                              | Coverage type                |
| --------------- | ------------------------------------------- | ------------------------------------------------------------------------------ | ---------------------------- |
| Query emptiness | q missing or empty string                   | 4xx error or an empty result list (depending on validation).                   | Service + controller         |
| Query emptiness | q non-empty                                 | Normal search behavior.                                                        | Service + controller         |
| Term count      | Single term (e.g. hello)                    | Matches rows containing that term.                                             | Service + controller         |
| Term count      | Multiple terms without OR                   | Interpreted as AND-style or multi-match search (implementation-dependent).     | Service                      |
| OR logic        | Contains OR operator (e.g. hello OR second) | Matches rows with either side of OR.                                           | Service + controller (cloud) |
| OR logic        | Only OR / malformed expression              | Results in empty list or degraded behavior (no crash).                         | Service (defensive behavior) |
| Phrase          | Contains quotes (e.g. "seed tweet")         | Phrase search logic applied when supported; tests check stability of behavior. | Service unit tests           |

Boundaries: maximum reasonable query length, special characters, presence of quotes without closing quote.

---

## **2.2 Pagination (limit, offset)**

**Dimensions:**

* limit value
* offset value

**Input dimension table**

| Dimension | Partition                 | Expected behavior                                            | Coverage type        |
| --------- | ------------------------- | ------------------------------------------------------------ | -------------------- |
| limit     | Positive (e.g. 5, 10, 20) | Returns up to limit results.                                 | Service + controller |
| limit     | Zero / negative           | Clamped to safe default or results in empty list (no crash). | Service-level tests  |
| offset    | Zero                      | Returns first page.                                          | Service + controller |
| offset    | Positive within range     | Skips that many results, returns following items.            | Service-level tests  |
| offset    | Beyond end of list        | Returns empty list.                                          | Service-level tests  |

---

## **2.3 Data source: timeline vs DB**

**Dimensions:**

* app.search.source

**Input dimension table**

| Dimension     | Partition | Expected behavior                                       | Coverage type                      |
| ------------- | --------- | ------------------------------------------------------- | ---------------------------------- |
| Search source | timeline  | Hits Mastodon timeline, returns real posts.             | Integration tests (cloud) + manual |
| Search source | db        | Uses seeded H2 data, returns deterministic demo tweets. | Integration tests (local)          |

DB-mode partitions are verified with:

```bash
mvn -q spring-boot:run -Dspring-boot.run.arguments="--app.search.source=db"

BASE='http://localhost:8080'
ACCOUNT='test-account'

curl -s "$BASE/search?accountId=$ACCOUNT&q=hello&limit=10" | jq
curl -s "$BASE/search/hashtags?accountId=$ACCOUNT&q=%23db&limit=10" | jq
curl -s "$BASE/search?accountId=$ACCOUNT&q=a&limit=20" | jq
```

---

## **2.4 Account state**

**Dimensions:**

* Valid vs invalid account / token

**Input dimension table**

| Dimension  | Partition                 | Expected behavior                                             | Coverage type       |
| ---------- | ------------------------- | ------------------------------------------------------------- | ------------------- |
| Auth state | Valid token for accountId | Search returns posts from that account’s timeline or DB.      | Integration (cloud) |
| Auth state | No token / invalid token  | Either 4xx error or empty result, depending on configuration. | Integration         |

---

# **3. Hashtag Search – /search/hashtags**

Hashtag search is a specialization of search.

## **3.1 Hashtag pattern**

**Dimensions:**

* Presence of #
* Case sensitivity

**Input dimension table**

| Dimension    | Partition                                  | Expected behavior                                           | Coverage type                |
| ------------ | ------------------------------------------ | ----------------------------------------------------------- | ---------------------------- |
| Query format | Contains # (e.g. #db)                      | Normal hashtag search, results where that hashtag appears.  | Service + controller         |
| Query format | No # in q                                  | Empty result or treated as non-hashtag query (no crash).    | Service + controller         |
| Case         | Lowercase vs uppercase (#DB vs #db)        | Matches are case-insensitive.                               | Service unit tests           |
| Source       | DB mode (app.search.source=db)             | Uses seeded demo tweets with #db.                           | Integration (local)          |
| Source       | Timeline mode (app.search.source=timeline) | Uses real Mastodon posts; may legitimately return empty []. | Integration (cloud) + manual |

---

# **4. Analytics – /analytics/***

Analytics run over the fetched/cached posts. Key endpoints:

* /analytics/sentiment
* /analytics/top-hashtags
* /analytics/best-hours
* /analytics/summary

## **4.1 Sentiment**

**Dimensions:**

* Number of tweets
* Distribution of sentiment scores

**Input dimension table**

| Dimension     | Partition       | Expected behavior                                               | Coverage type        |
| ------------- | --------------- | --------------------------------------------------------------- | -------------------- |
| Tweet count   | totalTweets = 0 | All counts zero, averageScore = 0 or neutral.                   | Service tests        |
| Tweet count   | totalTweets > 0 | Positive/negative/neutral counts computed, non-trivial average. | Integration + manual |
| Sentiment mix | All positive    | positive = totalTweets, others zero.                            | Service unit tests   |
| Sentiment mix | All negative    | negative = totalTweets, others zero.                            | Service unit tests   |
| Sentiment mix | Mixed           | Mix of counts, average between negative and positive extremes.  | Service unit tests   |

---

## **4.2 Top hashtags**

**Dimensions:**

* Presence of hashtags
* n parameter

**Input dimension table**

| Dimension | Partition                | Expected behavior                           | Coverage type |
| --------- | ------------------------ | ------------------------------------------- | ------------- |
| Hashtags  | No hashtags in any tweet | topHashtags empty list.                     | Service tests |
| Hashtags  | Some hashtags present    | topHashtags includes them with frequencies. | Service tests |
| n         | Positive n (e.g. 3, 5)   | Returns up to n entries.                    | Service tests |
| n         | Zero or negative n       | Clamped or returns empty list.              | Service tests |

---

## **4.3 Best hours**

**Dimensions:**

* Hours with posts vs no posts

**Input dimension table**

| Dimension    | Partition                             | Expected behavior                      | Coverage type         |
| ------------ | ------------------------------------- | -------------------------------------- | --------------------- |
| Distribution | No tweets at all                      | Empty map or all zeros.                | Service tests         |
| Distribution | Tweets concentrated at specific hours | Map with counts higher at those hours. | Service + integration |

---

## **4.4 Summary**

Summary endpoint essentially combines partitions from sentiment, hashtags, and best-hours.

**Input dimension table**

| Dimension   | Partition       | Expected behavior                                     | Coverage type |
| ----------- | --------------- | ----------------------------------------------------- | ------------- |
| Tweet count | totalTweets = 0 | topHashtags empty, bestHourUtc absent or default.     | Service tests |
| Tweet count | totalTweets > 0 | topHashtags and bestHourUtc populated if data exists. | Service tests |

---

# **5. Scheduling – /tools/schedule_tweet**

## **5.1 Time parameter**

time is an ISO-8601 UTC timestamp string.

**Dimensions:**

* Valid vs invalid format
* Past vs future

**Input dimension table**

| Dimension  | Partition                                       | Expected behavior                                                   | Coverage type        |
| ---------- | ----------------------------------------------- | ------------------------------------------------------------------- | -------------------- |
| Format     | Valid ISO-8601, UTC (e.g. 2025-12-03T07:56:14Z) | Request accepted, job persisted, status="scheduled".                | Integration + NL     |
| Format     | Malformed timestamp                             | 4xx error (validation failure).                                     | Controller tests     |
| Time value | Time clearly in the future                      | Scheduled job executed later by background worker.                  | Integration + manual |
| Time value | Time in the past / near-past                    | Rejected or executed immediately, but must not crash the scheduler. | Service tests        |

---

## **5.2 Account and text**

**Dimensions:**

* accountId
* text

**Input dimension table**

| Dimension | Partition                  | Expected behavior                                      | Coverage type        |
| --------- | -------------------------- | ------------------------------------------------------ | -------------------- |
| accountId | Valid, with existing token | Job stored; when executed, posts to Mastodon.          | Integration + manual |
| accountId | No token / invalid account | Rejected at execution or flagged as failure; no crash. | Service tests        |
| text      | Non-empty                  | Scheduled normally.                                    | Integration + NL     |
| text      | Empty / whitespace         | 4xx validation error or rejected by service layer.     | Service/controller   |

The NL client generates valid requests for the “happy-path” partitions (non-empty text, valid future time, valid account).

---

# **6. Audit – /audit/***

Two endpoints:

* /audit/recent?limit=...
* /audit/summary?hours=...

## **6.1 Recent**

**Dimensions:**

* limit value
* Presence of data

**Input dimension table**

| Dimension | Partition                  | Expected behavior                       | Coverage type          |
| --------- | -------------------------- | --------------------------------------- | ---------------------- |
| limit     | Positive (limit > 0)       | Returns up to limit entries.            | Integration + NL tests |
| limit     | Zero / negative            | Treated as 0 or clamped; returns [].    | Service tests          |
| Data      | No audit records           | Returns empty [].                       | Integration tests      |
| Data      | Some audit records present | Returns recent entries ordered by time. | Integration tests      |

---

## **6.2 Summary**

**Dimensions:**

* hours value
* Presence of data

**Input dimension table**

| Dimension | Partition                    | Expected behavior                                | Coverage type     |
| --------- | ---------------------------- | ------------------------------------------------ | ----------------- |
| hours     | Positive (e.g. 24)           | Aggregates calls over last hours.                | Integration tests |
| hours     | Zero / negative              | Treated as 0 or clamped, typically empty result. | Service tests     |
| Data      | No audit records in window   | All counts zero or empty map.                    | Integration tests |
| Data      | Some audit records in window | Non-zero counts for tools that were used.        | Integration tests |

---

# **7. MCP Tools – /mcp**

Two primary methods:

* tools/list
* tools/call

## **7.1 tools/list**

Partitions are simple:

**Input dimension table**

| Dimension    | Partition                  | Expected behavior                          | Coverage type |
| ------------ | -------------------------- | ------------------------------------------ | ------------- |
| Request body | Well-formed JSON-RPC       | Returns list of tools in result.tools.     | Integration   |
| Request body | Malformed JSON-RPC payload | JSON-RPC error response (no server crash). | Service tests |

---

## **7.2 tools/call**

**Dimensions:**

* Tool name
* Arguments

**Input dimension table**

| Dimension | Partition                                | Expected behavior                                    | Coverage type |
| --------- | ---------------------------------------- | ---------------------------------------------------- | ------------- |
| Tool name | Known tool (e.g. search_tweets)          | Executes underlying service, returns result.         | Integration   |
| Tool name | Unknown tool                             | JSON-RPC error with appropriate error code.          | Service tests |
| Arguments | Well-formed, all required fields present | Success path; delegated to search/scheduling/etc.    | Integration   |
| Arguments | Missing / invalid arguments              | JSON-RPC error or validation error; no server crash. | Service tests |

MCP layer shares many partitions with the underlying REST endpoints (search, scheduling, token tools).

---

# **8. DB vs Timeline Source – Global Partition**

Many features implicitly have a global partition based on app.search.source:

**Global partition table**

| Source            | Partition | Impact on behavior                                               |
| ----------------- | --------- | ---------------------------------------------------------------- |
| app.search.source | timeline  | Uses live Mastodon API for search/analytics; data is dynamic.    |
| app.search.source | db        | Uses embedded H2 TWEETS table; data is seeded and deterministic. |

Tests cover both:

Timeline mode – integration and manual tests against Railway.

DB mode – local integration tests and manual checks using seeded demo records.

---

# **9. Natural-Language Client – Partitions (High-Level)**

The NL client does not expose HTTP endpoints directly, but it has its own partitions for interpreting user text.

## **9.1 Intent detection**

**Dimensions:**

* Recognized intent phrases
* Unrecognized input

**Input dimension table**

| Dimension | Partition                                                                                 | Expected behavior                                               | Coverage type |
| --------- | ----------------------------------------------------------------------------------------- | --------------------------------------------------------------- | ------------- |
| Intent    | Clear phrases (“log in”, “schedule a tweet”, “show sentiment”, “what are the best hours”) | Produces corresponding CLI args and calls the correct endpoint. | Unit tests    |
| Intent    | Ambiguous or unsupported phrasing                                                         | Returns “Could not understand that instruction.”                | Unit/manual   |

---

## **9.2 Message & numeric extraction**

**Dimensions:**

* Presence of quoted text
* Time expression presence

**Input dimension table**

| Dimension        | Partition                                       | Expected behavior                                       | Coverage type |
| ---------------- | ----------------------------------------------- | ------------------------------------------------------- | ------------- |
| Tweet message    | Message inside quotes                           | Uses quoted text as text.                               | Unit tests    |
| Tweet message    | Message after words like “saying” / “that says” | Extracts tail as text.                                  | Unit tests    |
| Tweet message    | No message / time-only instruction              | Returns an error asking for tweet text (no scheduling). | Unit tests    |
| Time expressions | “in X minutes” / “in X seconds”                 | Converts to appropriate --in-minutes / --in-seconds.    | Unit tests    |
| Numbers          | Numeric words (one, two, three)                 | Parsed into integers.                                   | Unit tests    |

The NL client is responsible for the “happy-path” partitions; server-side tests still cover invalid or malformed inputs.

---

# **10. Summary**

For each major feature (auth, search, hashtags, analytics, scheduling, audit, MCP tools, NL client), we defined equivalence partitions and boundary cases.

These partitions are reflected in:

* Service-level unit tests (e.g., search logic, analytics, OAuth client).
* HTTP/controller integration tests for /search, /search/hashtags, /analytics/*, /audit/*, /tools/schedule_tweet, /auth/*, and /mcp.
* Manual end-to-end tests using the Python NL client and curl, both locally and on the Railway cloud deployment.
