# OAuth Implementation for MCP Server

Hey! So I've been working on adding OAuth authentication to our MCP server. Harry already built a solid foundation with the MCP protocol and quota management, so I built on top of that.

## What I Built

Basically, I needed to add two main things:
1. **OAuth token management** - storing and retrieving tokens securely
2. **Persistent storage with caching** - using Redis for performance

### Database Stuff

I created three main entities:

**OAuthToken** - This stores the actual OAuth tokens. The tricky part was making sure tokens are encrypted before going into the database. Nobody wants plain text tokens sitting around.

**Client** - Manages different client applications. Each client gets their own rate limits and settings.

**UserProfile** - Stores Twitter user info like followers, tweets, etc. This will be useful when we actually integrate with Twitter's API.

### Services

**OAuthTokenService** - Handles all the token operations. It automatically encrypts tokens when saving and decrypts when retrieving. Also has some cleanup logic for expired tokens.

**CacheService** - Redis integration for caching timelines and user profiles. Also handles rate limiting per client.

### MCP Tools

I added three new tools that can be called through the MCP protocol:

- `store_oauth_token` - Save a token for a client/user combo
- `get_oauth_token` - Retrieve a token (automatically decrypted)
- `delete_oauth_token` - Remove a token

### Configuration

Added Redis to the docker-compose setup and configured encryption using Jasypt. The encryption key comes from environment variables so it's not hardcoded.

## Database Tables

Here's what the tables look like (Spring Boot creates them automatically):

```sql
-- OAuth tokens - stores encrypted tokens
CREATE TABLE oauth_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    access_token VARCHAR(2000) NOT NULL, -- encrypted
    refresh_token VARCHAR(2000), -- encrypted
    token_type VARCHAR(50),
    expires_at TIMESTAMP,
    scope VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Clients - manages different client apps
CREATE TABLE clients (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_id VARCHAR(255) NOT NULL UNIQUE,
    client_name VARCHAR(255) NOT NULL,
    client_secret VARCHAR(255) NOT NULL, -- encrypted
    redirect_uri VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    rate_limit_per_hour INTEGER DEFAULT 1000,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- User profiles - Twitter user info
CREATE TABLE user_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    bio VARCHAR(2000),
    profile_image_url VARCHAR(500),
    followers_count INTEGER DEFAULT 0,
    following_count INTEGER DEFAULT 0,
    tweet_count INTEGER DEFAULT 0,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    last_sync_at TIMESTAMP
);
```

## Security Stuff

**Token Encryption** - All tokens get encrypted before hitting the database. I'm using Jasypt for this, and the encryption key comes from environment variables.

**Rate Limiting** - Each client gets their own rate limit bucket in Redis. Default is 1000 requests per hour, but it's configurable.

**Multi-Tenant** - Clients are completely isolated from each other. No client can see another client's tokens or data.

## Testing

I created a test script (`test-oauth.sh`) that tests all the OAuth functionality. It stores a token, retrieves it, tries to get a non-existent one, and then deletes it.

To run it:
```bash
# Start Redis
docker-compose up -d redis

# Start the app
mvn spring-boot:run

# Run tests (need jq installed)
./test-oauth.sh
```

## Configuration

The main config is in `application.properties`. Redis connection details and encryption settings go there.

For environment variables, you'll want to set:
- `JASYPT_PASSWORD` - encryption key for tokens
- `REDIS_HOST` - Redis server (defaults to localhost)

## How It Fits Together

This builds on Harry's work. I didn't change any of the existing MCP protocol stuff - just added new tools and services. All the existing functionality still works exactly the same.

The caching helps with performance - timelines get cached for 15 minutes, user profiles for an hour. Rate limiting prevents any single client from hammering the API.

## Files I Added/Changed

**New files:**
- All the model classes (OAuthToken, Client, UserProfile)
- Repository interfaces
- OAuthTokenService and CacheService
- Redis and encryption config classes
- test-oauth.sh

**Modified files:**
- pom.xml (added dependencies)
- application.properties (Redis config)
- McpService.java (added OAuth tools)
- docker-compose.yml (added Redis)

## What's Next

This gives us a solid foundation for OAuth. When we integrate with Twitter's API, we can use these stored tokens to make authenticated requests. The caching will help reduce API calls, and the rate limiting will keep us within Twitter's limits.

The system is ready to handle multiple clients securely, with each client having their own isolated token storage and rate limits.
