# Search & Discovery

## What it does

Lets you query tweets by **keywords**, **quoted phrases**, **AND/OR**, and exact **hashtags**.
It can read from either:

* **DB mode**: the `tweets` table (when `app.search.source=db`, which we use in the `devdb` profile).
* **Timeline mode**: the in-memory FakeTwitterClient (when `app.search.source=memory`, the default).

Results are ranked by (1) match score (phrases count double) and then (2) recency.

---

## Where the code lives (this is your actual directory)

`src/main/java/com/team/mcp/search/`

* `SearchService.java` — the core search engine: parsing, scoring, and choosing the source (DB vs timeline).
* `SearchController.java` — HTTP endpoints:

  * `GET /search` for keywords/phrases with AND/OR and pagination
  * `GET /search/hashtags` for exact hashtag matches
* `SearchQuery.java` — query parser (splits on `OR`, supports phrases in quotes, AND within each clause).
* `SearchTweetsTool.java` — MCP tool wrapper for search, so AI/clients can call it via `/mcp` → `tools/call`.
  *(Yes, this file is in the **search** package in your tree.)*
* `TweetEntity.java` — JPA entity mapped to table `tweets` (used in DB mode).
* `TweetRepository.java` — JPA repository helpers (simple finders, not required for the core search path).
* `TweetIngestor.java` — service that pulls from a TwitterClient and stores into `tweets`.
* `SeedTweetsRunner.java` — optional seeding runner (only when the `seed` profile is active).
* `package-info.java` — package docs for Checkstyle.

**Related, but in other packages:**

* `src/main/java/com/team/mcp/twitter/` — `Tweet` DTO, `TwitterClient`, `FakeTwitterClient` (timeline mode data source).
* `src/main/resources/db/migration/` — Flyway migrations (e.g., `V2__tweets.sql`) that create `tweets` and indexes.

---

## How to run (DB mode, persistent H2 file DB)

We’ve been using a persistent H2 database in file mode so data survives restarts.

1. Start the app in DB mode:

```bash
mvn -q spring-boot:run \
  -Dspring-boot.run.profiles=devdb \
  -Dspring-boot.run.jvmArguments="-Dapp.search.source=db"
```

2. H2 Console (optional):

* URL: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
* JDBC URL: `jdbc:h2:file:./.h2/mcp;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL`
* User: `sa`   Password: *(empty)*

3. The DB contains the same sample rows we used (`db-1`, `db-2`, `db-3`). You can insert/delete from the console.

> Switch to timeline mode by omitting the JVM arg or setting `-Dapp.search.source=memory`. Endpoints don’t change—only the source.

---

## HTTP endpoints — runs we already did (with one-line explanations)

**AND search** (both tokens must appear):

```bash
curl -s "http://localhost:8080/search?accountId=acctA&q=db%20tweet&limit=10" | jq .
```

Observed:

```json
[
  {"id":"db-1","user":"user0","text":"Hello from DB tweet #1","createdAt":"2025-10-19T09:29:54.006593Z"},
  {"id":"db-3","user":"user2","text":"Another DB tweet \"seed tweet\"","createdAt":"2025-10-19T09:27:54.006593Z"}
]
```

Explanation: both rows contain “db” and “tweet”; ranking is by score then recency.

**Hashtag search** (must include `#`, case-insensitive exact token match):

```bash
curl -s "http://localhost:8080/search/hashtags?accountId=acctA&q=%23db&limit=5" | jq .
```

Observed:

```json
[
  {"id":"db-2","user":"user1","text":"This has #db and hello","createdAt":"2025-10-19T09:28:54.006593Z"}
]
```

Explanation: returns rows containing the exact token `#db`.

**Phrase search**:

```bash
curl -s "http://localhost:8080/search?accountId=acctA&q=%22seed%20tweet%22&limit=5" | jq .
```

Observed:

```json
[
  {"id":"db-3","user":"user2","text":"Another DB tweet \"seed tweet\"","createdAt":"2025-10-19T09:27:54.006593Z"}
]
```

Explanation: exact phrase match for `"seed tweet"`.

**OR search** (phrase OR token):

```bash
curl -s "http://localhost:8080/search?accountId=acctA&q=hello%20OR%20%22seed%20tweet%22&limit=5" | jq .
```

Observed (order may vary slightly by recency):

```json
[
  {"id":"db-3","user":"user2","text":"Another DB tweet \"seed tweet\"","createdAt":"2025-10-19T09:27:54.006593Z"},
  {"id":"db-1","user":"user0","text":"Hello from DB tweet #1","createdAt":"2025-10-19T09:29:54.006593Z"},
  {"id":"db-2","user":"user1","text":"This has #db and hello","createdAt":"2025-10-19T09:28:54.006593Z"}
]
```

Explanation: matches rows that contain either `hello` or the phrase `"seed tweet"`.

**Validation on hashtag endpoint** (missing `#`):

```bash
curl -si "http://localhost:8080/search/hashtags?accountId=acctA&q=52"
```

Observed: `HTTP/1.1 400` with body `{"error":"query must start with '#'"}`
Explanation: endpoint enforces proper hashtag input.

**Pagination example**:

```bash
curl -s "http://localhost:8080/search?accountId=acctA&q=hello&offset=5&limit=5" | jq .
```

Observed: `[]`
Explanation: our sample set is small; page 2 is empty.

---

## MCP tool usage (same search engine, called via /mcp)

**AND search via MCP tool `search_tweets`:**

```bash
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{
        "jsonrpc":"2.0",
        "id":1,
        "method":"tools/call",
        "params":{
          "name":"search_tweets",
          "arguments":{
            "accountId":"acctA",
            "q":"db tweet",
            "offset":0,
            "limit":10
          }
        }
      }' | jq .
```

Explanation: this is how an AI client (or any MCP client) asks the server to run a search.

**Hashtag via MCP tool:**

```bash
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{
        "jsonrpc":"2.0",
        "id":2,
        "method":"tools/call",
        "params":{
          "name":"search_tweets",
          "arguments":{
            "accountId":"acctA",
            "q":"#db",
            "limit":5
          }
        }
      }' | jq .
```

Explanation: same rule as the HTTP endpoint—`q` must include `#`.

---

## Managing data quickly (dev tips)

* Inspect:

  ```sql
  SELECT * FROM tweets ORDER BY created_at DESC;
  ```
* Insert:

  ```sql
  INSERT INTO tweets(id, account_id, user_handle, text, created_at)
  VALUES ('x1','acctA','user9','hello from DB #sample', CURRENT_TIMESTAMP());
  ```
* Delete:

  ```sql
  DELETE FROM tweets WHERE id = 'db-2';
  ```

These are safe in the `devdb` profile and persist across restarts (H2 file DB).

---

## What’s planned for Iteration 2

1. **Live Twitter API**: replace FakeTwitterClient with real calls, optionally hydrate recent results into the `tweets` table.
2. **Better ranking**: tokenization/stop-words, improved phrase boundaries, and per-field boosts (e.g., prefer matches in `user_handle`).
3. **Richer filters**: date ranges, from:user, hashtags + keywords, exclusions (`-term`).
4. **Indexes at scale**: Postgres `GIN/tsvector`; keep H2 indexes simple for dev.
5. **Cross-tenant scope**: search across multiple accounts and paginate reliably.
6. **MCP ergonomics**: add search options to the tool arguments and return structured rows for UI rendering.

---

