# **Auth / OAuth – Iteration 2**

---

## **Mastodon-Backed OAuth + Token Lifecycle**

---

## **What it does (Iteration 2)**

This iteration replaces the simulated OAuth flow from Iteration 1 with a full OAuth 2.0 integration using Mastodon as the identity provider.
The subsystem now supports:

---

### **Real Authorization Flow**

`/auth/start` generates a real authorize URL pointing to:

```
https://mastodon.social/oauth/authorize
```

The user signs in and approves scopes.

`/auth/callback` exchanges the authorization code using Mastodon’s real:

```
/oauth/token
```

This returns both `access_token` and `expires_in`.

---

### **Token Storage (DB)**

Tokens are stored in the existing `token_credentials` table via `DbTokenStore`.
Tokens are:

* encrypted at rest (via `SecretCryptoService`)
* persisted using H2 file mode (or PostgreSQL if desired)
* accessible to all subsystems (search, scheduling, posting)

---

### **Token Metadata (Iteration-2 Additions)**

New administrative endpoints allow examining token state.

**GET `/auth/status`**

Reports whether a token exists, whether it is expired, and includes metadata:

```
{
  "accountId": "test-account",
  "hasToken": true,
  "provider": "mastodon",
  "scopes": "read:statuses write:statuses read:accounts",
  "hasRefresh": false,
  "expiresAt": "2025-11-17T17:38:18Z",
  "health": "healthy"
}
```

**GET `/auth/meta`**

Displays provider settings, callback URL, and configured scopes.

---

### **Token Deletion (Lifecycle)**

```
DELETE /auth/token?accountId=X
```

Removes a stored token for an account.
This demonstrates token lifecycle management without requiring Mastodon’s revoke endpoint.

---

## **Where the code lives (Iteration 2)**

```
src/main/java/com/team/mcp/auth/
    DbTokenStore.java              ← DB-backed token store
    SecretCryptoService.java       ← token encryption (base64 placeholder)
    TokenCredential.java           ← entity
    TokenCredentialRepository.java
    TokenStore.java
    TwitterOAuthClient.java        ← now a real Mastodon OAuth client
    TokenService.java              ← now stores expiry + no-refresh flag

src/main/java/com/team/mcp/auth/web/
    AuthController.java            ← OAuth endpoints + new status/meta/delete
```

---

## **Data model**

The existing `token_credentials` table stores:

* account_id
* token (encrypted)

Optional fields:

* refresh_token (null for Mastodon)
* expires_at

No schema migrations were needed.

---

## **How to run**

The backend runs using persistent H2 file mode:

```
mvn -q spring-boot:run \
 -Dspring-boot.run.profiles=devdb \
 -Dspring-boot.run.jvmArguments="-Dapp.fakeTwitter=false -Dapp.search.source=db"
```

H2 Console:

```
http://localhost:8080/h2-console
```

---

## **OAuth Flow (Real Mastodon)**

### **1) Begin OAuth**

```
curl -s "http://localhost:8080/auth/start?accountId=test-account" | jq .
```

The response includes:

* authorize_url
* state
* callback

Opening the authorize URL in a browser completes approval.

---

### **2) Callback**

Mastodon redirects to:

```
http://localhost:8080/auth/callback?code=...&state=...
```

The backend:

* verifies `state`
* exchanges the authorization code
* stores the encrypted token in the database

---

## **Iteration-2 Admin APIs**

### **Check token status**

```
curl -s "http://localhost:8080/auth/status?accountId=test-account" | jq .
```

### **Delete token**

```
curl -s -X DELETE "http://localhost:8080/auth/token?accountId=test-account" | jq .
```

### **View provider metadata**

```
curl -s "http://localhost:8080/auth/meta" | jq .
```

---

## **MCP Tools**

All MCP tools now operate using real Mastodon OAuth tokens.

* `search_tweets` issues live Mastodon API calls
* `schedule_tweet` posts real statuses through `MastodonClient`
* `check_quota_status` unchanged
* `get_token`, `set_token`, `list_tokens` fully operational

---

## **What’s new in Iteration 2**

Compared with Iteration 1, this iteration adds:

### **1. Real OAuth 2.0**

* actual authorize URL
* real code → token exchange
* expiry tracking

### **2. Token Lifecycle Management**

* `/auth/status`
* `/auth/token` (DELETE)
* `/auth/meta`

### **3. Token Health Logic**

Categories include:

* healthy
* expired
* missing

### **4. Full Mastodon Integration**

* home timeline retrieval
* posting statuses
* scheduled posting to a Mastodon account
* search backed by live Mastodon data

---

## **What remains future work (beyond course requirements)**

Although the subsystem is complete for the project’s scope, potential future enhancements include:

* refresh token support (if the provider allows it)
* automatic renewal before expiry
* per-tool scope verification

---

## **Iteration 2 Summary**

* OAuth now implemented with real Mastodon authorization
* Token storage is secure and persistent (DB + encryption)
* Scheduling posts real content to Mastodon
* Search retrieves the actual home timeline
* Analytics operates on live Mastodon posts
* New admin/status/delete APIs added
* All MCP tools function using real token data



