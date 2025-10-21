# Auth / OAuth

## What it does

* Stores per-account OAuth credentials in the DB (access/refresh tokens, expiry, provider).
* Exposes a minimal **web flow** (`/auth/start` → `/auth/callback`) that simulates a provider redirect and “token exchange”.
* Exposes **MCP tools** so an AI client (or your own script) can read/write/list tokens via JSON-RPC.
* Uses a pluggable `TokenStore` (`DbTokenStore`) and a “crypto” wrapper (`SecretCryptoService`) to keep secrets opaque at rest.

Tokens persist, can be listed/retrieved, and the flow exercises the plumbing you’ll reuse when you hook up a real provider.

---

## Where the code lives

```
src/main/java/com/team/mcp/auth/
  DbTokenStore.java           -- TokenStore impl backed by JPA repositories
  OAuthToken.java             -- small value object for provider exchange
  OAuthTokenRepository.java   -- (if persisted) repository for OAuthToken rows
  SecretCryptoService.java    -- placeholder “encryption” (base64) for secrets
  TenantAccount.java          -- entity for logical tenant/account
  TenantAccountRepository.java
  TokenCredential.java        -- entity holding provider + secret material
  TokenCredentialRepository.java
  TokenService.java           -- high-level API used by controllers/tools
  TokenStore.java             -- interface abstraction
  TwitterOAuthClient.java     -- stub client that simulates token exchange

src/main/java/com/team/mcp/auth/web/
  AuthController.java         -- /auth/start and /auth/callback endpoints
```

(Plus JPA / Flyway config under `src/main/resources/` as you set earlier. We run with the `devdb` profile so tokens persist in the H2 file DB.)

---

## Data model (what gets stored)

* **`token_credentials`** (via `TokenCredential`): `account_id`, `provider`, `access_token`, `refresh_token`, `expires_at`, `created_at`, `updated_at`.
* **`tenant_account`** (via `TenantAccount`): tracks your logical account IDs.
* Optionally **`oauth_tokens`** if you persisted raw exchanges (depends on your final mapping; the service typically maps provider responses straight into `token_credentials`).

Secrets are run through `SecretCryptoService` before persisting. Today that is base64 to satisfy “no plain text” at rest; swap in real encryption in Iteration 2.

---

## How to run (same app, DB mode)

Start the app in persistent H2 + DB source:

```bash
mvn -q spring-boot:run \
  -Dspring-boot.run.profiles=devdb \
  -Dspring-boot.run.jvmArguments="-Dapp.search.source=db"
```

Optional H2 console: `http://localhost:8080/h2-console`
JDBC URL: `jdbc:h2:file:./.h2/mcp;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL`
User: `sa`, Password: empty.

---

## Web flow (simulated)

### 1) Begin auth

```
GET /auth/start?accountId={acct}&provider=twitter
```

Example:

```bash
curl -s "http://localhost:8080/auth/start?accountId=acctA&provider=twitter" | jq .
```

You’ll get JSON with a mock `authUrl` and a `state` you’d normally be redirected to. For Iteration 1 you can copy the `state` and immediately “complete” the flow.

### 2) Complete auth (callback)

```
GET /auth/callback?code=fake-code&state={from-step-1}
```

Example:

```bash
STATE="paste-state-from-step-1"
curl -s "http://localhost:8080/auth/callback?code=dummy-code&state=${STATE}" | jq .
```

Expected result: object indicating success. Under the hood the controller calls `TwitterOAuthClient` (simulated exchange) → `TokenService.save(...)` → `DbTokenStore` → DB. You should see a new row in `token_credentials`.

Quick DB check (H2 console):

```sql
SELECT account_id, provider, LENGTH(access_token) AS len_access, expires_at
FROM token_credentials
ORDER BY created_at DESC;
```

---

## MCP tools (JSON-RPC) for tokens

The same tokens are accessible to AI clients (or your own scripts) via `/mcp`:

### Discover available tools

```bash
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{ "jsonrpc":"2.0","id":1,"method":"tools/list" }' | jq .
```

Look for entries like `list_tokens`, `get_token`, `set_token` (names may differ slightly—use this output as ground truth).

### List tokens for an account

```bash
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc":"2.0","id":2,"method":"tools/call",
    "params":{"name":"list_tokens","arguments":{"accountId":"acctA"}}
  }' | jq .
```

**What it does:** returns a minimal list (provider + expiry) so an AI can decide what it can call next.

### Get one provider’s token

```bash
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc":"2.0","id":3,"method":"tools/call",
    "params":{"name":"get_token","arguments":{"accountId":"acctA","provider":"twitter"}}
  }' | jq .
```

**What it does:** returns access token (and refresh/expiry if present) decrypted by `SecretCryptoService`. Callers must protect the output.

### Set/replace a token (manual inject)

```bash
curl -s http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -d "{
    \"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",
    \"params\":{\"name\":\"set_token\",\"arguments\":{
      \"accountId\":\"acctA\",
      \"provider\":\"twitter\",
      \"accessToken\":\"abc123\",
      \"refreshToken\":\"ref456\",
      \"expiresAt\":\"2025-12-31T00:00:00Z\"
    }}
  }" | jq .
```

**What it does:** upserts a credential row for the account/provider. Useful in tests or when backfilling tokens.

> If the exact argument names differ in your tool class, grab them from `tools/list` output first; we designed `McpService` to be discoverable.

---

## Typical test sequence we ran

1. Start app in DB mode (command above).
2. Hit `/auth/start` for `acctA`, copy `state`.
3. Hit `/auth/callback` with `state` and a dummy `code`.
4. `tools/list` then `list_tokens` for `acctA` → see one row for `twitter`.
5. `get_token` for `acctA` + `twitter` → see decrypted values.
6. Optionally `set_token` to overwrite and verify DB updated.

---

## Notes, gotchas, and security posture (Iteration 1)

* **Encryption**: `SecretCryptoService` uses base64 as a placeholder. It prevents plain text in the DB but is not real crypto. Iteration 2 will swap it for KMS or a proper AES-GCM implementation with key rotation.
* **Providers**: `TwitterOAuthClient` simulates the OAuth exchange. The flow shape is real (state, code, exchange), the network hop is mocked.
* **DDL**: In `devdb` we run Flyway for core tables and keep token entities simple. If a teammate starts with an empty DB and token tables are missing, either:

  * add a small Flyway migration for `token_credentials`/`tenant_account`, or
  * temporarily set `spring.jpa.hibernate.ddl-auto=update` to bootstrap, then switch back to `none`.
* **Multi-tenant**: Tokens are keyed by `accountId` (“tenant” in code). The same server can hold multiple tenants’ credentials without collision.

---

## What’s expected in Iteration 2

1. **Real OAuth 2.0** with Twitter/X (or another provider):

   * Replace the simulated `TwitterOAuthClient` with actual authorize/token endpoints.
   * Refresh-token rotation and automatic refresh in `TokenService`.
   * Proper scopes and consent screens.

2. **Admin and lifecycle**:

   * Revoke/delete endpoints.
   * Token health checks and expiry alerts.
   * Optional per-tool access checks based on available scopes.

---

## Quick reference (paths and classes)

* Endpoints:

  * `GET /auth/start` → `AuthController.start(...)`
  * `GET /auth/callback` → `AuthController.callback(...)`

* Service layer:

  * `TokenService` → `DbTokenStore` → `TokenCredentialRepository`

* MCP integration:

  * `/mcp` (JSON-RPC) → `McpService` → token tools (see `tools/list`)

