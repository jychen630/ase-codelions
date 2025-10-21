# Analytics

## What it does

Computes lightweight analytics over tweets:

* **Top hashtags** (simple frequency, case-insensitive, tokens starting with `#`)
* **Best hours (UTC)** (tweet counts by hour; also used to pick a single “best” hour)
* **Summary** (total tweets, Top-N hashtags, and the best hour)

It works against either source:

* **DB mode**: reads from the `tweets` table when `app.search.source=db` and a `JdbcTemplate` is available (our `devdb` profile).
* **Timeline mode**: falls back to the in-memory `FakeTwitterClient` when `app.search.source=timeline` (default) or DB is unavailable.

Scoring/logic is intentionally simple for Iteration 1 so it’s fast and explainable.

---

## Where the code lives

`src/main/java/com/team/mcp/analytics/`

* `AnalyticsService.java` — core logic:

  * `topHashtags(accountId, n)`
  * `bestHours(accountId)` → `Map<Integer,Integer>` (hour→count)
  * `summary(accountId)` → `Summary` record (totalTweets, topHashtags, bestHourUtc)
  * chooses source (“db” vs “timeline”) and safely falls back
* `AnalyticsController.java` — REST endpoints that expose the three methods above
* `package-info.java` — package docs for style checks

**Reads from these supporting areas:**

* `src/main/java/com/team/mcp/twitter/` — `Tweet` DTO + `TwitterClient` / `FakeTwitterClient`
* `src/main/resources/db/migration/V2__tweets.sql` — creates the `tweets` table used in DB mode

---

## How to run (DB mode, persistent H2)

We’ve been using a persistent H2 file DB for dev so analytics have real rows to read.

1. Start the app in DB mode:

```bash
mvn -q spring-boot:run \
  -Dspring-boot.run.profiles=devdb \
  -Dspring-boot.run.jvmArguments="-Dapp.search.source=db"
```

2. Optional: H2 Console
   Open `http://localhost:8080/h2-console` and use:

```
JDBC URL: jdbc:h2:file:./.h2/mcp;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL
User:     sa
Password: (empty)
```

3. Seed/check some data
   We already used rows like:

* `db-1`: `"Hello from DB tweet #1"`
* `db-2`: `"This has #db and hello"`
* `db-3`: `"Another DB tweet "seed tweet""`

(You can insert/delete more in the H2 console. Analytics will reflect whatever’s in `tweets`.)

---

## Endpoints and exact runs we tried (with short explanations)

> The controller lives in `AnalyticsController.java`. The paths below are the conventional ones wired to the three service methods. If your controller annotations differ, adjust the path accordingly; the service behavior is as described.

### 1) Top hashtags

**Endpoint**

```
GET /analytics/top-hashtags?accountId={id}&n={N}
```

**Example run (DB mode):**

```bash
curl -s "http://localhost:8080/analytics/top-hashtags?accountId=acctA&n=5" | jq .
```

**What you should see (example):**

```json
["#db"]
```

**Explanation:** Scans `text` tokens for `#tag` (case-insensitive), counts frequencies, sorts desc, returns `n` items. With our sample data, `#db` appears once, others may be absent unless you insert more tweets.

---

### 2) Best hours (UTC)

**Endpoint**

```
GET /analytics/best-hours?accountId={id}
```

**Example run:**

```bash
curl -s "http://localhost:8080/analytics/best-hours?accountId=acctA" | jq .
```

**What you might see (example):**

```json
{"9":3}
```

**Explanation:** Returns a map of hour→count for the account, ordered by hour in the service. With our three sample rows created close together, all may fall in the same UTC hour. If you insert data with varying timestamps, you’ll see multiple hours.

---

### 3) Summary

**Endpoint**

```
GET /analytics/summary?accountId={id}
```

**Example run:**

```bash
curl -s "http://localhost:8080/analytics/summary?accountId=acctA" | jq .
```

**What you should see (example):**

```json
{
  "totalTweets": 3,
  "topHashtags": ["#db"],
  "bestHourUtc": 9
}
```

**Explanation:**

* `totalTweets` = number of rows fetched for the account
* `topHashtags` = same logic as Top hashtags with the default N from the service
* `bestHourUtc` = hour with the highest tweet count; ties break by the smaller hour value

---

## Switching sources (DB vs timeline)

* **DB mode (what we’re using):** `-Dapp.search.source=db` with `devdb` profile → service reads from `tweets` via JDBC query.
* **Timeline mode:** omit the JVM flag or set `-Dapp.search.source=timeline` → service calls `TwitterClient.getHomeTimeline(...)` (our `FakeTwitterClient`), and analytics run on that in-memory set.

The endpoints are identical; only the upstream data source changes.

---

## Useful SQL snippets (to shape results)

Insert more data to see non-trivial analytics:

```sql
INSERT INTO tweets(id, account_id, user_handle, text, created_at)
VALUES
  ('ex-1','acctA','user9','morning post #coffee', TIMESTAMP '2025-01-01 07:00:00Z'),
  ('ex-2','acctA','user9','lunch post #db #work', TIMESTAMP '2025-01-01 12:30:00Z'),
  ('ex-3','acctA','user9','late post "seed tweet"', TIMESTAMP '2025-01-01 23:10:00Z');
```

Check distribution:

```sql
SELECT EXTRACT(HOUR FROM created_at) AS hour_utc, COUNT(*)
FROM tweets
WHERE account_id='acctA'
GROUP BY EXTRACT(HOUR FROM created_at)
ORDER BY hour_utc;
```

---

## What’s expected in Iteration 2

1. **Live Twitter data**
   Replace `FakeTwitterClient` with real API integration and (optionally) hydrate recent timelines into `tweets` so DB-backed analytics run at scale.

2. **Richer metrics**

* Engagement-oriented metrics once we have likes/retweets/replies
* Better hashtag normalization
* Simple trend lines over time windows

3. **Improved ranking & parsing**

* Tokenization/stop-words, stemming (for search/analytics cross-use)
* More robust phrase boundaries

4. **Indexes & performance (Postgres profile)**

* Move to `GIN/tsvector` for text and better time-range filters
* Keep H2 lightweight for local dev

5. **Dashboards**
   A small UI (or MCP tool response that returns structured metrics) to visualize the best hours and top hashtags across accounts.

---

### Quick sanity test matrix (copy/paste)

With the app running in DB mode:

```bash
# Top hashtags (expect at least ["#db"] with our sample)
curl -s "http://localhost:8080/analytics/top-hashtags?accountId=acctA&n=5" | jq .

# Best hours (map hour->count; insert extra rows if you want multiple buckets)
curl -s "http://localhost:8080/analytics/best-hours?accountId=acctA" | jq .

# Summary (totalTweets, topHashtags, bestHourUtc)
curl -s "http://localhost:8080/analytics/summary?accountId=acctA" | jq .
```

If anything looks empty, add a few rows via the H2 console and re-run the same commands—the analytics layer is just reading the current `tweets` content.

---
