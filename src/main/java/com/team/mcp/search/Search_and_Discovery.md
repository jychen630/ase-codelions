# **Search & Discovery — Iteration 2 (Final Version)**

---

## **What it does now**

In Iteration 2, the Search & Discovery module is upgraded to operate over real Mastodon timelines rather than simulated in-memory data.
All search capabilities from Iteration 1 remain fully supported:

* keyword search
* quoted phrase search
* boolean AND / OR
* exact hashtag matches (#tag)
* ranking by relevance + recency
* pagination via offset + limit
* MCP tool access via `/mcp` tools/call

The primary improvement is that search is now performed over live remote Mastodon statuses fetched through our `MastodonClient`, while still preserving the same search engine and ranking logic from Iteration 1.

This allows the system to behave like a real social-media search backend, while the `SearchService` itself required no rewriting—only the underlying client changed.

---

## **What changed from Iteration 1 → Iteration 2**

### **1. Real Mastodon timeline as the data source**

Instead of the `FakeTwitterClient` timeline, Iteration 2 uses:

```
MastodonClient.getHomeTimeline(accountId, count)
```

This retrieves live statuses using the stored OAuth token and converts them into the internal `Tweet` DTO.

Our existing `SearchService` operates on these statuses with no required modifications.

---

### **2. Full compatibility with phrase / AND / OR parsing**

Because the parsing and scoring logic was already cleanly structured, all features work identically with real Mastodon content:

```
"Hello"
"seed tweet"
hello AND project
hello OR second
#db
```

---

### **3. MCP tool search_tweets upgraded automatically**

No MCP changes were required.
`search_tweets` continues to call `SearchService`, which now uses real Mastodon data under the hood.

---

### **4. Backward compatibility preserved**

DB-mode search still works when:

```
app.search.source=db
```

is enabled, but Iteration 2 primarily operates in timeline mode, retrieving Mastodon posts on demand.

---

## **Where the iteration-2 code lives**

All code remains in the same locations as Iteration 1, with no structural refactoring:

```
src/main/java/com/team/mcp/search/SearchService.java
SearchController.java
SearchQuery.java
SearchTweetsTool.java
MastodonClient.java     (new iteration-2 client replacing fake mode)
Tweet.java
TweetEntity.java
```

---

## **How to test the iteration-2 improvements**

Below is a suite of curl commands demonstrating search working over real Mastodon data.
These are the same commands we previously executed successfully.

---

### **1. Ensure authentication succeeded**

```
curl -s "http://localhost:8080/auth/start?accountId=test-account" | jq
```

After approving in the browser, the callback stores the token.

Verify stored token:

```
curl -s "http://localhost:8080/auth/status?accountId=test-account" | jq
```

Expected:

```
{
  "accountId": "test-account",
  "hasToken": true,
  "provider": "mastodon",
  "scopes": "read:statuses write:statuses read:accounts",
  "health": "healthy"
}
```

---

### **2. Keyword search (real Mastodon posts)**

```
curl -s "http://localhost:8080/search?accountId=test-account&q=Hello&limit=10" | jq
```

Observed result:

```
[
  {"id": "115558990098402186", "user": "ase_project", "text": "Hello from Mastodon scheduler (real)"},
  {"id": "115555727686799693", "user": "ase_project", "text": "Hello! This is the forth text ..."},
  {"id": "115555397730635524", "user": "ase_project", "text": "Hello from Mastodon scheduler (real)"}
]
```

---

### **3. Boolean OR search**

```
curl -s "http://localhost:8080/search?accountId=test-account&q=hello%20OR%20second&limit=10" | jq
```

Returns posts containing either term.

---

### **4. Hashtag search (real Mastodon content)**

```
curl -s "http://localhost:8080/search/hashtags?accountId=test-account&q=%23db&limit=5" | jq
```

Example output (if posts contain `#db`):

```
[
  {"id": "demo-2", "user": "user1", "text": "This has #db and hello"}
]
```

---

### **5. MCP tool test — search via JSON-RPC**

```
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{
        "jsonrpc":"2.0",
        "id":1,
        "method":"tools/call",
        "params":{
          "name":"search_tweets",
          "arguments":{
            "accountId":"test-account",
            "q":"hello",
            "limit":5
          }
        }
      }' | jq
```

Example response:

```
{
  "result": {
    "content": [
      {
        "type": "text",
        "text": "115558990098402186 | ase_project | Hello from Mastodon scheduler (real)\n..."
      }
    ]
  }
}
```

This demonstrates that:

```
MCP → SearchTweetsTool → SearchService → MastodonClient → real data
```

works consistently end-to-end.

---

### **6. Pagination (same behavior as Iteration 1)**

```
curl -s "http://localhost:8080/search?accountId=test-account&q=a&offset=5&limit=5" | jq
```

Observed output:

```
[]
```

This is correct when fewer than five results exist beyond the first page.

---

## **Final Status — Search & Discovery (Iteration 2: Complete)**

* Works with real Mastodon timeline data
* Phrase, keyword, AND/OR, and hashtag search fully supported
* MCP tool integration continues to function correctly
* Pagination unchanged and fully operational
* Compatible with DB-mode fallback
* Produces correct results using our real Mastodon posts
* Meets the practical Iteration-2 goals planned in Iteration 1
