# Scheduling

## What it does

The Scheduling feature lets us queue a tweet to be posted at a specific future time. At run time, a lightweight scheduler loop scans the database for due rows and “posts” them via the current `TwitterClient` (in Iteration-1 this is the `FakeTwitterClient`). Each job moves through a simple lifecycle:

`PENDING` → (at/after `run_at`) → post via client → `POSTED` (or `FAILED` on error)

The scheduler is **persistent**: work survives restarts because rows live in the DB.

---

## Where the code lives

```
src/main/java/com/team/mcp/scheduling/
  ScheduledPost.java            -- JPA entity (id, accountId, text, runAt, status, postedTweetId, createdAt, updatedAt)
  ScheduledPostRepository.java  -- Spring Data repository
  SchedulingService.java        -- create/find/mark-posted/mark-failed, core business logic
  SchedulerRunner.java          -- the polling loop that picks up due rows and executes them
  package-info.java

src/main/java/com/team/mcp/config/
  SchedulingConfig.java         -- @EnableScheduling / scheduler wiring
  ClockConfig.java              -- unified time source (Clock bean) used by scheduler/service
  FakeTwitterConfig.java        -- wires the FakeTwitterClient used in Iteration-1

src/main/java/com/team/mcp/tools/
  ToolsController.java          -- convenience REST wrapper to call a tool by name, e.g. /tools/schedule_tweet

src/main/java/com/team/mcp/twitter/
  TwitterClient.java            -- client abstraction (real later)
  FakeTwitterClient.java        -- Iteration-1 “post” implementation (generates a fake tweet id)
  dto/Tweet.java                -- Tweet DTO used across features
```

**Database schema**
Created by Flyway (Iteration-1) in `V1__init.sql`:

```sql
CREATE TABLE IF NOT EXISTS scheduled_posts (
  id              BIGSERIAL PRIMARY KEY,     -- H2 maps this to a big integer identity
  account_id      VARCHAR(128)   NOT NULL,
  text            VARCHAR(1000)  NOT NULL,
  run_at          TIMESTAMP      NOT NULL,    -- H2-compatible (PG uses TIMESTAMPTZ in prod)
  status          VARCHAR(16)    NOT NULL,    -- PENDING | POSTED | FAILED
  posted_tweet_id VARCHAR(128),
  created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP(),
  updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

CREATE INDEX IF NOT EXISTS idx_scheduled_status_runat ON scheduled_posts (status, run_at);
CREATE INDEX IF NOT EXISTS idx_scheduled_account     ON scheduled_posts (account_id);
```

---

## How to run it

Use the DB profile so scheduled jobs persist and Flyway creates tables:

```bash
mvn -q spring-boot:run \
  -Dspring-boot.run.profiles=devdb \
  -Dspring-boot.run.jvmArguments="-Dapp.search.source=db"
```

H2 console: `http://localhost:8080/h2-console`
JDBC URL: `jdbc:h2:file:./.h2/mcp;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL`
User: `sa`  (empty password)

---

## How to schedule a tweet (REST helper)

We expose a small helper endpoint so you don’t have to craft full MCP JSON-RPC while developing:

```
POST /tools/schedule_tweet
Content-Type: application/json
Body: { "tool": "schedule_tweet", "params": { "text": "...", "time": "<ISO-8601 UTC>", "accountId": "acctA" } }
```

Notes:

* `time` must be ISO-8601 (`YYYY-MM-DDTHH:MM:SSZ` or with an explicit offset). We treat it as UTC.
* `accountId` defaults to `acctA` if omitted in Iteration-1 (adjust if your local code requires it).
* The endpoint enqueues one row in `scheduled_posts` with `status=PENDING`.

### Example: schedule ~40 seconds in the future

```bash
FUTURE=$(date -u -d '+40 seconds' --iso-8601=seconds)

curl -s http://localhost:8080/tools/schedule_tweet \
  -H 'Content-Type: application/json' \
  -d "{ \"tool\": \"schedule_tweet\",
        \"params\": {\"text\": \"hello from scheduler\",
                     \"time\": \"${FUTURE}\",
                     \"accountId\":\"acctA\"} }" | jq
```

**What you’ll see**

* HTTP 200 with a small JSON payload (tool result).
* One new row in `scheduled_posts`:

```sql
SELECT id, account_id, text, run_at, status, posted_tweet_id, created_at, updated_at
FROM scheduled_posts
ORDER BY id DESC
LIMIT 5;
```

Initially: `status = PENDING`, `posted_tweet_id = NULL`.

### After the due time passes

The scheduler loop (see `SchedulerRunner`) will pick the row, call `TwitterClient.post(...)` (Fake client now), then update the row:

* `status = POSTED`
* `posted_tweet_id = 'fake-...'` (whatever the fake client returns)
* `updated_at` is refreshed

Run:

```sql
SELECT id, status, posted_tweet_id, updated_at
FROM scheduled_posts
ORDER BY id DESC
LIMIT 5;
```

### Immediate run (time in the past)

If you pass a past timestamp, the job is eligible right away:

```bash
PAST=$(date -u -d '-5 seconds' --iso-8601=seconds)
curl -s http://localhost:8080/tools/schedule_tweet \
  -H 'Content-Type: application/json' \
  -d "{ \"tool\": \"schedule_tweet\",
        \"params\": {\"text\": \"run now\", \"time\": \"${PAST}\", \"accountId\":\"acctA\"} }" | jq

# Give it 1–2 seconds, then:
SELECT id, status, posted_tweet_id FROM scheduled_posts ORDER BY id DESC LIMIT 3;
```

You should already see `POSTED`.

### Minimal validation errors

* Missing `text` → 400 (tool returns an error payload)
* Bad timestamp format → 400

Your console also logs each HTTP request and the tool call is written to the `tool_call_audit` table (Audit feature).

---

## Code layout (scheduling feature only)

* `src/main/java/com/team/mcp/scheduling/`

  * `ScheduledPost.java` – JPA entity for a scheduled job (fields: id, accountId, text, runAt, status, postedTweetId, createdAt, updatedAt). Includes state transitions `markPosted(...)` and `markFailed()`.
  * `ScheduledPostRepository.java` – Spring Data repository. Provides `findDue(Instant now, Status status, Pageable p)` to page through due PENDING jobs ordered by time.
  * `SchedulingService.java` – the service that coordinates persistence and publishing.

    * **schedule(text, runAt, accountId)** → validates inputs and stores a `PENDING` row.
    * **publisherTick()** → scans `PENDING` rows that are due (via `repo.findDue(...)`), calls the `TwitterClient` to post, and marks rows `POSTED` or `FAILED`.
  * `SchedulerRunner.java` – lightweight runner that periodically invokes `SchedulingService.publisherTick()` (uses Spring’s scheduling).
* `src/main/java/com/team/mcp/config/`

  * `SchedulingConfig.java` – enables scheduling and wires the periodic tick.
  * `ClockConfig.java` – provides a `Clock` bean; the service uses it so tests can inject a fixed time.
  * `FakeTwitterConfig.java` – wires the fake `TwitterClient` used in Iteration-1.

### DB migrations (Flyway)

* `db/migration/V1__init.sql` – creates `scheduled_posts` plus useful indexes.
* We’re using H2 in “file” mode during dev, so data survives restarts.

---

## Verifying end-to-end with the DB

1. **Queue a few jobs**:

```bash
FUTURE1=$(date -u -d '+30 seconds' --iso-8601=seconds)
FUTURE2=$(date -u -d '+45 seconds' --iso-8601=seconds)

for i in 1 2; do
  TS_VAR="FUTURE${i}"
  curl -s http://localhost:8080/tools/schedule_tweet \
    -H 'Content-Type: application/json' \
    -d "{ \"tool\": \"schedule_tweet\",
          \"params\": {\"text\": \"batch $i\", \"time\": \"${!TS_VAR}\", \"accountId\":\"acctA\"} }" > /dev/null
done
```

2. **Watch them flip**:

```sql
SELECT id, text, run_at, status, posted_tweet_id
FROM scheduled_posts
ORDER BY id DESC;
```

Refresh after ~1 minute. You should see both rows transition to `POSTED`.

3. **Optional: confirm a posted “tweet” is discoverable**
   If your `SchedulingService` (or fake client) also writes to `tweets` for DB-backed search:

```bash
curl -s "http://localhost:8080/search?accountId=acctA&q=batch&limit=5" | jq .
```

You should see the generated IDs and the scheduled texts.

---

## Admin operations (manual)

We’ve kept Iteration-1 simple. If you need to adjust a job by hand:

* **Reschedule**:

  ```sql
  UPDATE scheduled_posts
  SET run_at = TIMESTAMP '2025-12-31T23:59:00', updated_at = CURRENT_TIMESTAMP()
  WHERE id = ? AND status = 'PENDING';
  ```

* **Cancel** (soft delete):

  ```sql
  UPDATE scheduled_posts
  SET status = 'FAILED', posted_tweet_id = NULL, updated_at = CURRENT_TIMESTAMP()
  WHERE id = ? AND status = 'PENDING';
  ```

---

## Configuration notes

* **Polling cadence:** configured in `SchedulingConfig` (fixed delay). For demos we keep it fast (≈1s). For production, increase the delay and add batching.
* **Time zone:** we treat incoming `time` as UTC. The DB stores `TIMESTAMP` in H2; in PostgreSQL we’ll use `TIMESTAMPTZ`.
* **Idempotency:** Iteration-1 assumes “at most once” per row; Iteration-2 will add guards to prevent duplicate posts if a tick overlaps.

---

## What changes in Iteration 2

* **Real posting:** swap `FakeTwitterClient` for a real Twitter/X client and use the per-tenant OAuth tokens (from the `auth` feature).
* **Cancellation & listing API:** `/schedule/list`, `/schedule/cancel/{id}`, `/schedule/reschedule/{id}` plus MCP tools.
* **Retries & backoff:** move `FAILED` jobs into a retry queue with capped exponential backoff and dead-letter logging.
* **Concurrency controls:** per-tenant throttling to respect API rate limits.
* **Recurring & drafts:** support simple recurrence (e.g., every weekday 10:00) and scheduling from saved drafts.
* **Observability:** counters and p95 duration for “time to post”, Prometheus/metrics endpoints, and dashboard tiles.

---

## TL;DR

* Start with `devdb` profile.
* Use `POST /tools/schedule_tweet` to enqueue a job (pass `text`, `time`, `accountId`).
* Verify in H2: `scheduled_posts` goes `PENDING` → `POSTED` after the time.
* For search integration, query `/search` for the scheduled text if your local build writes to the `tweets` table after posting.
