#!/bin/bash

# Test script for OAuth token management
# Make sure the server is running on localhost:18060

echo "=== Testing OAuth Token Management ==="
echo

# Test 1: Store OAuth token
echo "1. Storing OAuth token..."
curl -X POST http://localhost:18060/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "store_oauth_token",
      "arguments": {
        "client_id": "test_client_123",
        "user_id": "user_456",
        "access_token": "ya29.a0AfH6SMC...",
        "refresh_token": "1//04...",
        "expires_in": 3600
      }
    },
    "id": 1
  }' | jq '.'
echo

# Test 2: Retrieve OAuth token
echo "2. Retrieving OAuth token..."
curl -X POST http://localhost:18060/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "get_oauth_token",
      "arguments": {
        "client_id": "test_client_123",
        "user_id": "user_456"
      }
    },
    "id": 2
  }' | jq '.'
echo

# Test 3: Try to get non-existent token
echo "3. Trying to get non-existent token..."
curl -X POST http://localhost:18060/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "get_oauth_token",
      "arguments": {
        "client_id": "nonexistent_client",
        "user_id": "nonexistent_user"
      }
    },
    "id": 3
  }' | jq '.'
echo

# Test 4: List available tools
echo "4. Listing available tools..."
curl -X POST http://localhost:18060/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/list",
    "params": {},
    "id": 4
  }' | jq '.result.tools[].name'
echo

# Test 5: Delete OAuth token
echo "5. Deleting OAuth token..."
curl -X POST http://localhost:18060/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "delete_oauth_token",
      "arguments": {
        "client_id": "test_client_123",
        "user_id": "user_456"
      }
    },
    "id": 5
  }' | jq '.'
echo

# Test 6: Verify token is deleted
echo "6. Verifying token is deleted..."
curl -X POST http://localhost:18060/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "get_oauth_token",
      "arguments": {
        "client_id": "test_client_123",
        "user_id": "user_456"
      }
    },
    "id": 6
  }' | jq '.'
echo

echo "=== OAuth Token Management Tests Complete ==="
