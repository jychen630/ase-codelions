# Twitter MCP Server

A **Model Context Protocol (MCP)** server for Twitter/X integration, built with Java and Spring Boot. This server exposes Twitter functionality to AI agents and clients through a standardized JSON-RPC protocol.

## 🌟 Features

### Phase 1 - Core Functionality (Current)

- ✅ **MCP JSON-RPC Protocol** - Full implementation of MCP protocol specification
- ✅ **Quota Management** - Track and enforce Twitter API rate limits (100 reads/month, 500 writes/month)
- ✅ **Database Persistence** - H2 (development) and PostgreSQL (production) support
- ✅ **RESTful HTTP Interface** - Easy integration with any MCP-compatible client
- ✅ **Built-in Tools**:
  - `check_quota_status` - Monitor API usage
  - `echo_test` - Test MCP connectivity

### Phase 2 - Twitter Integration (Planned)

- 🔄 OAuth 2.0 authentication with Twitter
- 🔄 Post tweets (text + images)
- 🔄 Read home timeline
- 🔄 Search tweets
- 🔄 User interactions (like, retweet, reply)
- 🔄 User profile retrieval

### Phase 3 - Advanced Features (Future)

- 📊 Engagement analytics
- 🤖 Sentiment analysis
- 📈 Trending hashtag extraction (TF-IDF)
- 📅 Tweet scheduling
- 📝 Draft management

## 🏗️ Architecture

```
┌─────────────────┐
│  MCP Clients    │ (Claude, Cursor, AI Agents)
└────────┬────────┘
         │ JSON-RPC over HTTP
         ▼
┌──────────────────────────────────┐
│   Twitter MCP Server             │
│  ┌────────────────────────────┐  │
│  │   MCP Controller           │  │
│  │   (HTTP Endpoint)          │  │
│  └──────────┬─────────────────┘  │
│             ▼                     │
│  ┌────────────────────────────┐  │
│  │   MCP Service              │  │
│  │   (Protocol Handler)       │  │
│  └──────────┬─────────────────┘  │
│             ▼                     │
│  ┌────────────────────────────┐  │
│  │   Tool Handlers            │  │
│  │   - Quota Management       │  │
│  │   - Twitter API Client     │  │
│  └──────────┬─────────────────┘  │
│             ▼                     │
│  ┌────────────────────────────┐  │
│  │   Database (H2/PostgreSQL) │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **PostgreSQL** (optional, H2 used by default)

### Installation

1. **Clone the repository** (or download the source code)

```bash
cd twitter-mcp-server
```

2. **Build the project**

```bash
mvn clean install
```

3. **Run the server**

```bash
mvn spring-boot:run
```

The server will start on `http://localhost:18060`

### Configuration

Edit `src/main/resources/application.properties` to configure:

```properties
# Server port
server.port=18060

# Database (H2 by default)
spring.datasource.url=jdbc:h2:file:./data/twitter-mcp-db

# Twitter API credentials (set via environment variables)
twitter.api.key=${TWITTER_API_KEY:}
twitter.api.secret=${TWITTER_API_SECRET:}
twitter.access.token=${TWITTER_ACCESS_TOKEN:}
twitter.access.secret=${TWITTER_ACCESS_SECRET:}

# Quota limits
quota.read.max=100
quota.write.max=500
quota.reset.period=monthly
```

### Environment Variables

Set your Twitter API credentials:

```bash
export TWITTER_API_KEY="your_api_key"
export TWITTER_API_SECRET="your_api_secret"
export TWITTER_ACCESS_TOKEN="your_access_token"
export TWITTER_ACCESS_SECRET="your_access_secret"
```

## 📖 Usage

### Testing with curl

**1. Initialize the MCP connection**

```bash
curl -X POST http://localhost:18060/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"initialize","params":{},"id":1}'
```

**2. List available tools**

```bash
curl -X POST http://localhost:18060/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","params":{},"id":2}'
```

**3. Check quota status**

```bash
curl -X POST http://localhost:18060/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"check_quota_status","arguments":{}},"id":3}'
```

**4. Test echo**

```bash
curl -X POST http://localhost:18060/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"echo_test","arguments":{"message":"Hello MCP!"}},"id":4}'
```

### Using with MCP Inspector

The MCP Inspector is a great tool for testing and debugging MCP servers.

```bash
# Install MCP Inspector
npm install -g @modelcontextprotocol/inspector

# Run the inspector
npx @modelcontextprotocol/inspector
```

Then configure the inspector to connect to `http://localhost:18060/mcp`

### Integrating with Claude Code CLI

```bash
# Add the MCP server
claude mcp add --transport http twitter-mcp http://localhost:18060/mcp

# Verify it's added
claude mcp list

# Use it in Claude
# The tools will be automatically available to Claude
```

## 🛠️ Development

### Project Structure

```
twitter-mcp-server/
├── src/
│   ├── main/
│   │   ├── java/com/twitter/mcp/
│   │   │   ├── controller/       # HTTP controllers
│   │   │   ├── service/          # Business logic
│   │   │   ├── model/            # JPA entities
│   │   │   ├── repository/       # Data access
│   │   │   ├── dto/              # Data transfer objects
│   │   │   ├── exception/        # Custom exceptions
│   │   │   └── config/           # Configuration classes
│   │   └── resources/
│   │       └── application.properties
│   └── test/                     # Unit and integration tests
├── pom.xml                       # Maven configuration
└── README.md
```

### Running Tests

```bash
# Run all tests
mvn test

# Run with coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Code Quality

```bash
# Run Checkstyle
mvn checkstyle:check

# Run PMD
mvn pmd:check

# Run all quality checks
mvn clean verify
```

## 🔧 Technology Stack

- **Java 17** - Programming language
- **Spring Boot 2.7.18** - Application framework
- **Spring Data JPA** - Database access
- **H2 Database** - Development database
- **PostgreSQL** - Production database
- **Twitter4J** - Twitter API client
- **Lombok** - Reduce boilerplate code
- **JUnit 5** - Unit testing
- **Mockito** - Mocking framework
- **JaCoCo** - Code coverage
- **Checkstyle** - Code style checking
- **PMD** - Static code analysis

## 📊 API Quota Management

The server implements intelligent quota management to stay within Twitter's Free tier limits:

- **Read Operations**: 100 per month
- **Write Operations**: 500 per month
- **Reset Period**: Monthly (1st of each month)

### Quota Tracking

All API calls are tracked in the database. When quota is exceeded, the server returns an error:

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32000,
    "message": "Read quota exceeded: 100/100 used. Resets at 2025-11-01T00:00"
  },
  "id": 5
}
```

## 🧪 Testing Strategy

### Unit Tests

Test individual components in isolation using Mockito:

```java
@Test
public void testQuotaManagement() {
    // Mock dependencies
    when(mockRepository.findActiveQuota(any())).thenReturn(Optional.of(quota));
    
    // Test quota check
    quotaService.checkAndIncrementRead();
    
    // Verify behavior
    verify(mockRepository).save(any(QuotaUsage.class));
}
```

### Integration Tests

Test the full MCP protocol flow:

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class McpIntegrationTest {
    @Test
    public void testMcpInitialize() {
        // Send MCP request
        ResponseEntity<McpResponse> response = restTemplate.postForEntity(
            "/mcp", 
            new McpRequest("initialize", Map.of(), 1),
            McpResponse.class
        );
        
        // Assert response
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
```

## 🚦 Health Checks

The server exposes health check endpoints via Spring Boot Actuator:

```bash
# Health check
curl http://localhost:18060/actuator/health

# Metrics
curl http://localhost:18060/actuator/metrics

# Info
curl http://localhost:18060/actuator/info
```

## 📝 MCP Protocol Reference

### Supported Methods

| Method | Description | Parameters |
|--------|-------------|------------|
| `initialize` | Initialize MCP connection | None |
| `tools/list` | List available tools | None |
| `tools/call` | Execute a tool | `name`, `arguments` |

### Tool Definitions

#### check_quota_status

Check current API quota usage.

**Input**: None

**Output**:
```json
{
  "content": [{
    "type": "text",
    "text": "Quota Status:\n- Reads: 95/100 remaining\n- Writes: 450/500 remaining\n- Reset period: monthly"
  }]
}
```

#### echo_test

Echo back a message (for testing).

**Input**:
```json
{
  "message": "Hello World"
}
```

**Output**:
```json
{
  "content": [{
    "type": "text",
    "text": "Echo: Hello World"
  }]
}
```

## 🐛 Troubleshooting

### Server won't start

- Check if port 18060 is already in use: `lsof -i :18060`
- Verify Java version: `java -version` (should be 17+)
- Check logs in `server.log` or console output

### Database errors

- Delete the database file: `rm -rf data/`
- Restart the server to recreate the database

### Quota not resetting

- Check the database: Access H2 console at `http://localhost:18060/h2-console`
- Manually reset: Call the reset endpoint (admin only)

## 📚 References

- [Model Context Protocol Specification](https://spec.modelcontextprotocol.io/)
- [Twitter API v2 Documentation](https://developer.twitter.com/en/docs/twitter-api)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Xiaohongshu MCP Reference Implementation](https://github.com/xpzouying/xiaohongshu-mcp)

## 🤝 Contributing

This is a student project for educational purposes. Contributions, issues, and feature requests are welcome!

## 📄 License

This project is for educational purposes only. Not licensed for commercial use.

## ✨ Acknowledgments

- Inspired by the [Xiaohongshu MCP Server](https://github.com/xpzouying/xiaohongshu-mcp)
- Built for a software engineering course project
- Thanks to the MCP community for the protocol specification

---

**Status**: Phase 1 Complete ✅ | Phase 2 In Progress 🔄

**Last Updated**: October 13, 2025

